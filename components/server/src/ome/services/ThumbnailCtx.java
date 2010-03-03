/*
 *   $Id$
 *
 *   Copyright 2010 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services;

import java.awt.Dimension;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.StopWatch;
import org.perf4j.commonslog.CommonsLogStopWatch;

import ome.api.IPixels;
import ome.api.IQuery;
import ome.api.IRenderingSettings;
import ome.api.IUpdate;
import ome.conditions.ResourceError;
import ome.conditions.ValidationException;
import ome.io.nio.ThumbnailService;
import ome.model.core.Pixels;
import ome.model.display.RenderingDef;
import ome.model.display.Thumbnail;
import ome.model.internal.Details;
import ome.parameters.Parameters;
import ome.security.SecuritySystem;

/**
 *
 */
public class ThumbnailCtx
{
    /** Logger for this class. */
    private static final Log log = LogFactory.getLog(ThumbnailCtx.class);

    /** Default thumbnail MIME type. */
    public static final String DEFAULT_MIME_TYPE = "image/jpeg";

    /** OMERO query service. */
    private IQuery queryService;

    /** OMERO update service. */
    private IUpdate updateService;

    /** OMERO pixels service. */
    private IPixels pixelsService;

    /** ROMIO thumbnail service. */
    private ThumbnailService thumbnailService;

    /** OMERO rendering settings service. */
    private IRenderingSettings settingsService;

    /** OMERO security system for this session. */
    private SecuritySystem securitySystem;

    /** Current user ID. */
    private long userId;

    /** Pixels ID vs. Pixels object map. */
    private Map<Long, Pixels> pixelsIdPixelsMap =
        new HashMap<Long, Pixels>();

    /** Pixels ID vs. RenderingDef object map. */
    private Map<Long, RenderingDef> pixelsIdSettingsMap =
        new HashMap<Long, RenderingDef>();

    /** Pixels ID vs. Thumbnail object map. */
    private Map<Long, Thumbnail> pixelsIdMetadataMap =
        new HashMap<Long, Thumbnail>();

    /**
     * Pixels ID vs. RenderingDef object last modified time map. We don't access
     * these RenderingDef object properties directly due to load/unload issues
     * with Hibernate (ObjectUnloadedExceptions) when multiple objects were
     * created with or updated by the same event.
     */
    private Map<Long, Timestamp> pixelsIdSettingsLastModifiedTimeMap =
        new HashMap<Long, Timestamp>();

    /**
     * Pixels ID vs. Thumbnail object  last modified time map. We don't access
     * these Thumbnail object properties directly due to load/unload issues
     * with Hibernate (ObjectUnloadedExceptions) when multiple objects were
     * created with or updated by the same event.
     */
    private Map<Long, Timestamp> pixelsIdMetadataLastModifiedTimeMap =
        new HashMap<Long, Timestamp>();

    /**
     * Pixels ID vs. RenderingDef object owner. We don't access these
     * RenderingDef object properties directly due to load/unload issues
     * with Hibernate (ObjectUnloadedExceptions) when multiple objects were
     * created with or updated by the same event.
     */
    private Map<Long, Long> pixelsIdSettingsOwnerIdMap =
        new HashMap<Long, Long>();

    /**
     * Default constructor.
     * @param queryService OMERO query service to use.
     * @param updateService OMERO update service to use.
     * @param pixelsService OMERO pixels service to use.
     * @param settingsService OMERO rendering settings service to use. 
     * @param thumbnailService OMERO thumbnail service to use.
     * @param securitySystem OMERO security system for this session.
     * @param userId Current user ID.
     */
    public ThumbnailCtx(IQuery queryService, IUpdate updateService,
            IPixels pixelsService, IRenderingSettings settingsService,
            ThumbnailService thumbnailService, SecuritySystem securitySystem,
            long userId)
    {
        this.queryService = queryService;
        this.updateService = updateService;
        this.pixelsService = pixelsService;
        this.settingsService = settingsService;
        this.thumbnailService = thumbnailService;
        this.securitySystem = securitySystem;
        this.userId = userId;
    }

    /**
     * Bulk loads a set of rendering settings for a  group of pixels sets and
     * prepares our internal data structures.
     * @param pixelsIds Set of Pixels IDs to prepare rendering settings for.
     */
    public void loadAndPrepareRenderingSettings(Set<Long> pixelsIds)
    {
        // First populate our hash maps asking for our settings.
        List<RenderingDef> settingsList = bulkLoadRenderingSettings(pixelsIds);
        for (RenderingDef settings : settingsList)
        {
            prepareRenderingSettings(settings, settings.getPixels());
        }

        // Now check to see if we're in a state where missing settings requires
        // us to use the owner's settings (we're "graph critical") and load
        // them if possible.
        Set<Long> pixelsIdsWithoutSettings = 
            getPixelsIdsWithoutSettings(pixelsIds);
        if (securitySystem.isGraphCritical())
        {
            settingsList = 
                bulkLoadOwnerRenderingSettings(pixelsIdsWithoutSettings);
            for (RenderingDef settings : settingsList)
            {
                prepareRenderingSettings(settings, settings.getPixels());
            }
            pixelsIdsWithoutSettings = getPixelsIdsWithoutSettings(pixelsIds);
        }

        // For dimension pooling to work correctly for the purpose of thumbnail
        // metadata creation we now need to load the Pixels sets that had no
        // rendering settings.
        loadMissingPixels(pixelsIdsWithoutSettings);
    }

    /**
     * Loads and prepares a rendering settings for a Pixels ID and RenderingDef
     * ID.
     * @param pixelsId Pixels ID to load.
     * @param settingsId RenderingDef ID to load an prepare settings for.
     */
    public void loadAndPrepareRenderingSettings(long pixelsId, long settingsId)
    {
        Pixels pixels = pixelsService.retrievePixDescription(pixelsId);
        RenderingDef settings = pixelsService.loadRndSettings(settingsId);
        if (settings == null)
        {
            throw new ValidationException(
                    "No rendering definition exists with ID = " + settingsId);
        }
        if (!settingsService.sanityCheckPixels(pixels, settings.getPixels()))
        {
            throw new ValidationException(
                    "The rendering definition " + settingsId +
                    " is incompatible with pixels set " + pixels.getId());
        }
        prepareRenderingSettings(settings, pixels);
    }

    /**
     * Bulk loads and prepares metadata for a group of pixels sets. Calling
     * this method guarantees that metadata are available, creating them if
     * they are not.
     * @param pixelsIds Pixels IDs to prepare metadata for.
     * @param longestSide The longest side of the thumbnails requested.
     */
    public void loadAndPrepareMetadata(Set<Long> pixelsIds, int longestSide)
    {
        // Now we're going to attempt to efficiently retrieve the thumbnail
        // metadata based on our dimension pools above. To save significant
        // time later we're also going to pre-create thumbnail metadata where
        // it is missing.
        Map<Dimension, Set<Long>> dimensionPools = 
            createDimensionPools(longestSide);
        loadMetadataByDimensionPool(dimensionPools);
        createMissingThumbnailMetadata(dimensionPools);
    }

    /**
     * Bulk loads and prepares metadata for a group of pixels sets. Calling
     * this method guarantees that metadata are available, creating them if
     * they are not.
     * @param pixelsIds Pixels IDs to prepare metadata for.
     * @param dimensions X-Y dimensions of the thumbnails requested.
     */
    public void loadAndPrepareMetadata(Set<Long> pixelsIds,
                                       Dimension dimensions)
    {
        // Now we're going to attempt to efficiently retrieve the thumbnail
        // metadata based on our dimension pools above. To save significant
        // time later we're also going to pre-create thumbnail metadata where
        // it is missing.
        Map<Dimension, Set<Long>> dimensionPools = 
            new HashMap<Dimension, Set<Long>>();
        dimensionPools.put(dimensions, pixelsIds);
        loadMetadataByDimensionPool(dimensionPools);
        createMissingThumbnailMetadata(dimensionPools);
    }

    /**
     * Retrieves all thumbnail metadata available in the database for a given
     * Pixels ID.
     * @param pixelsId Pixels ID to retrieve thumbnail metadata for.
     * @return See above.
     */
    public List<Thumbnail> loadAllMetadata(long pixelsId)
    {
        Parameters params = new Parameters();
        params.addId(pixelsId);
        StopWatch s1 = new CommonsLogStopWatch("omero.loadAllMetadata");
        List<Thumbnail> toReturn = queryService.findAllByQuery(
                "select t from Thumbnail as t " +
                "join t.pixels " +
                "join fetch t.details.updateEvent " +
                "and t.details.owner.id = :o_id " +
                "and t.pixels.id in (:id)", params);
        s1.stop();
        return toReturn;
    }

    /**
     * Resets a given set of Pixels rendering settings to the default
     * effectively creating any which do not exist.
     * @param pixelsIds Pixels IDs 
     */
    public void createAndPrepareMissingRenderingSettings(Set<Long> pixelsIds)
    {
        // Now check to see if we're in a state where missing rendering
        // settings and our state requires us to not save.
        if (securitySystem.isGraphCritical())
        {
            // TODO: Could possibly "su" to the user and create a thumbnail
            return;
        }
        StopWatch s1 = new CommonsLogStopWatch(
                "omero.createAndPrepareMissingRenderingSettings");
        Set<Long> pixelsIdsWithoutSettings = 
            getPixelsIdsWithoutSettings(pixelsIds);
        int count = pixelsIdsWithoutSettings.size();
        if (count > 0)
        {
            log.info(count + " pixels without settings");
            Set<Long> resetPixelsIds = settingsService.resetDefaultsInSet(
                    Pixels.class, pixelsIdsWithoutSettings);
            loadAndPrepareRenderingSettings(resetPixelsIds);
        }
        s1.stop();
    }

    /**
     * Whether or not settings are available for a given Pixels ID.
     * @param pixelsId Pixels ID to check for availability.
     * @return <code>true</code> if settings are available and
     * <code>false</code> otherwise.
     */
    public boolean hasSettings(long pixelsId)
    {
        return pixelsIdSettingsMap.containsKey(pixelsId);
    }

    /**
     * Whether or not thumbnail metadata is available for a given Pixels ID.
     * @param pixelsId Pixels ID to check for availability.
     * @return <code>true</code> if metadata is available and
     * <code>false</code> otherwise.
     */
    public boolean hasMetadata(long pixelsId)
    {
        return pixelsIdMetadataMap.containsKey(pixelsId);
    }

    /**
     * Retrieves the Pixels object for a given Pixels ID.
     * @param pixelsId Pixels ID to retrieve the Pixels object for.
     * @return See above.
     */
    public Pixels getPixels(long pixelsId)
    {
        return pixelsIdPixelsMap.get(pixelsId);
    }

    /**
     * Retrieves the RenderingDef object for a given Pixels ID.
     * @param pixelsId Pixels ID to retrieve the RenderingDef object for.
     * @return See above.
     */
    public RenderingDef getSettings(long pixelsId)
    {
        return pixelsIdSettingsMap.get(pixelsId);
    }

    /**
     * Retrieves the Thumbnail object for a given Pixels ID.
     * @param pixelsId Pixels ID to retrieve the Thumbnail object for.
     * @return See above.
     */
    public Thumbnail getMetadata(long pixelsId)
    {
        return pixelsIdMetadataMap.get(pixelsId);
    }

    /**
     * Whether or not the thumbnail metadata for a given Pixels ID is dirty
     * (the RenderingDef has been updated since the Thumbnail was).
     * @param pixelsId Pixels ID to check for dirty metadata.
     * @return <code>true</code> if the metadata is dirty <code>false</code>
     * otherwise.
     */
    public boolean dirtyMetadata(long pixelsId)
    {
        Timestamp metadataLastUpdated = 
            pixelsIdMetadataLastModifiedTimeMap.get(pixelsId);
        Timestamp settingsLastUpdated =
            pixelsIdSettingsLastModifiedTimeMap.get(pixelsId);
        if (log.isDebugEnabled())
        {
            log.debug("Thumb time: " + metadataLastUpdated);
            log.debug("Settings time: " + settingsLastUpdated);
        }
        return settingsLastUpdated.after(metadataLastUpdated);
    }

    /**
     * Checks to see if a thumbnail is in the on disk cache or not.
     * 
     * @param pixelsId The Pixels set the thumbnail is for.
     * @return Whether or not the thumbnail is in the on disk cache.
     */
    public boolean isThumbnailCached(long pixelsId)
    {
        Thumbnail metadata = pixelsIdMetadataMap.get(pixelsId);
        try
        {
            if (dirtyMetadata(pixelsId)
                && thumbnailService.getThumbnailExists(metadata))
            {
                return true;
            }
        }
        catch (IOException e)
        {
            String s = "Could not check if thumbnail is cached: ";
            log.error(s, e);
            throw new ResourceError(s + e.getMessage());
        }
        return false;
    }

    /**
     * Calculates the ratio of the two sides of a Pixel set and returns the
     * X and Y widths based on the longest side maintaining aspect ratio.
     * 
     * @param pixels The Pixels set to calculate against.
     * @param longestSide The size of the longest side of the thumbnail
     * requested.
     * @return The calculated width (X) and height (Y).
     */
    public Dimension calculateXYWidths(Pixels pixels, int longestSide)
    {
        int sizeX = pixels.getSizeX();
        int sizeY = pixels.getSizeY();
        if (sizeX > sizeY)
        {
            float ratio = (float) longestSide / sizeX;
            return new Dimension(longestSide, (int) (sizeY * ratio));
        }
        float ratio = (float) longestSide / sizeY;
        return new Dimension((int) (sizeX * ratio), longestSide);
    }

    /**
     * Creates X-Y dimension pools based on a requested longest side.
     * @param longestSide Requested longest side of the thumbnail. 
     * @return Map of X-Y dimension vs. Pixels ID (a set of dimension pools).
     */
    private Map<Dimension, Set<Long>> createDimensionPools(int longestSide)
    {
        Map<Dimension, Set<Long>> dimensionPools =
            new HashMap<Dimension, Set<Long>>();
        for (Pixels pixels : pixelsIdPixelsMap.values())
        {
            // Calculate the XY widths we would use for a thumbnail of Pixels
            Dimension dimensions = calculateXYWidths(pixels, longestSide);
            addToDimensionPool(dimensionPools, pixels, dimensions);
        }
        return dimensionPools;
    }

    /**
     * Adds the Id of a particular set of Pixels to the correct dimension pool 
     * based on the requested longest side.
     * 
     * @param pools Map of the current dimension pools.
     * @param pixels Pixels set to add to the correct dimension pool.
     * @param dimensions Dimensions pool to add to.
     */
    private void addToDimensionPool(Map<Dimension, Set<Long>> pools,
            Pixels pixels, Dimension dimensions)
    {
        // Insert the Pixels set into the dimension pool
        Set<Long> pool = pools.get(dimensions);
        if (pool == null)
        {
            pool = new HashSet<Long>();
        }
        pool.add(pixels.getId());
        pools.put(dimensions, pool);
    }

    /**
     * Examines the currently prepared data structures for Pixels IDs without
     * settings.
     * @param pixelsIds Pixels IDs to check.
     * @return Set of Pixels IDs which do not have settings prepared.
     */
    private Set<Long> getPixelsIdsWithoutSettings(Set<Long> pixelsIds)
    {
        Set<Long> pixelsIdsWithoutSettings = new HashSet<Long>();
        for (Long pixelsId : pixelsIds)
        {
            if (!hasSettings(pixelsId))
            {
                pixelsIdsWithoutSettings.add(pixelsId);
            }
        }
        return pixelsIdsWithoutSettings;
    }

    /**
     * Examines the currently prepared data structures for Pixels IDs without
     * thumbnail metadata.
     * @param pixelsIds Pixels IDs to check.
     * @return Set of Pixels IDs which do not have thumbnail metadata prepared.
     */
    private Set<Long> getPixelsIdsWithoutMetadata(Set<Long> pixelsIds)
    {
        Set<Long> pixelsIdsWithoutMetadata = new HashSet<Long>();
        for (Long pixelsId : pixelsIds)
        {
            if (!hasMetadata(pixelsId))
            {
                pixelsIdsWithoutMetadata.add(pixelsId);
            }
        }
        return pixelsIdsWithoutMetadata; 
    }

    /**
     * Bulk loads a set of rendering sets for a group of pixels sets.
     * @param pixelsIds the Pixels sets to retrieve thumbnails for.
     * @return Loaded rendering settings for <code>pixelsIds</code>.
     */
    private List<RenderingDef> bulkLoadRenderingSettings(Set<Long> pixelsIds)
    {
        StopWatch s1 = new CommonsLogStopWatch(
                "omero.bulkLoadRenderingSettings");
        List<RenderingDef> toReturn = queryService.findAllByQuery(
                "select r from RenderingDef as r join fetch r.pixels " +
                "join fetch r.details.updateEvent " +
                "join fetch r.pixels.details.updateEvent " +
                "where r.details.owner.id = :id and r.pixels.id in (:ids)",
                new Parameters().addId(userId).addIds(pixelsIds));
        s1.stop();
        return toReturn;
    }

    /**
     * Bulk loads a set of rendering sets for a group of pixels sets.
     * @param pixelsIds the Pixels sets to retrieve thumbnails for.
     * @return Loaded rendering settings for <code>pixelsIds</code>.
     */
    private List<RenderingDef> bulkLoadOwnerRenderingSettings(
            Set<Long> pixelsIds)
    {
        StopWatch s1 = new CommonsLogStopWatch(
                "omero.bulkLoadOwnerRenderingSettings");
        List<RenderingDef> toReturn = queryService.findAllByQuery(
                "select r from RenderingDef as r join fetch r.pixels " +
                "join fetch r.details.updateEvent " +
                "join fetch r.pixels.details.updateEvent " +
                "where r.details.owner.id = p.details.owner.id " +
                "and r.pixels.id in (:ids)",
                new Parameters().addIds(pixelsIds));
        s1.stop();
        return toReturn;
    }

    /**
     * Bulk loads thumbnail metadata.
     * @param dimensions X-Y dimensions to bulk load metdata for.
     * @param pixelsIds Pixels IDs to bulk load metadata for.
     * @return List of thumbnail objects with <code>thumbnail.pixels</code> and
     * <code>thumbnail.details.updateEvent</code> loaded.
     */
    private List<Thumbnail> bulkLoadMetadata(Dimension dimensions,
                                             Set<Long> pixelsIds)
    {
        Parameters params = new Parameters();
        params.addInteger("x", (int) dimensions.getWidth());
        params.addInteger("y", (int) dimensions.getHeight());
        params.addLong("o_id", userId);
        params.addIds(pixelsIds);
        StopWatch s1 = new CommonsLogStopWatch("omero.bulkLoadMetadata");
        List<Thumbnail> toReturn = queryService.findAllByQuery(
                "select t from Thumbnail as t " +
                "join t.pixels " +
                "join fetch t.details.updateEvent " +
                "where t.sizeX = :x and t.sizeY = :y " +
                "and t.details.owner.id = :o_id " +
                "and t.pixels.id in (:ids)", params);
        s1.stop();
        return toReturn;
    }

    /**
     * Bulk loads thumbnail metadata that is owned by the owner of the Pixels
     * set..
     * @param dimensions X-Y dimensions to bulk load metadata for.
     * @param pixelsIds Pixels IDs to bulk load metadata for.
     * @return List of thumbnail objects with <code>thumbnail.pixels</code> and
     * <code>thumbnail.details.updateEvent</code> loaded.
     */
    private List<Thumbnail> bulkLoadOwnerMetadata(Dimension dimensions,
                                                  Set<Long> pixelsIds)
    {
        Parameters params = new Parameters();
        params.addInteger("x", (int) dimensions.getWidth());
        params.addInteger("y", (int) dimensions.getHeight());
        params.addIds(pixelsIds);
        StopWatch s1 = new CommonsLogStopWatch("omero.bulkLoadOwnerMetadata");
        List<Thumbnail> toReturn = queryService.findAllByQuery(
                "select t from Thumbnail as t " +
                "join t.pixels as p " + 
                "join fetch t.details.updateEvent " +
                "where t.sizeX = :x and t.sizeY = :y " +
                "and t.details.owner.id = p.details.owner.id " +
                "and t.pixels.id in (:ids)", params);
        s1.stop();
        return toReturn;
    }

    /**
     * Attempts to efficiently retrieve the thumbnail metadata based on a set
     * of dimension pools. At worst, the result of maintaining the aspect ratio
     * (calculating the new XY widths) is that we have to retrieve each 
     * thumbnail object separately.
     * @param dimensionPools Dimension pools to query based upon.
     * @param metadataMap Dictionary of Pixels ID vs. thumbnail metadata. Will
     * be updated by this method.
     * @param metadataTimeMap Dictionary of Pixels ID vs. thumbnail metadata
     * last modification time. Will be updated by this method.
     */
    private void loadMetadataByDimensionPool(
            Map<Dimension, Set<Long>> dimensionPools)
    {
        StopWatch s1 = new CommonsLogStopWatch(
                "omero.loadMetadataByDimensionPool");
        for (Dimension dimensions : dimensionPools.keySet())
        {
            Set<Long> pool = dimensionPools.get(dimensions);
            // First populate our hash maps asking for our metadata.
            List<Thumbnail> thumbnailList = bulkLoadMetadata(dimensions, pool);
            for (Thumbnail metadata : thumbnailList)
            {
                prepareMetadata(metadata, metadata.getPixels().getId());
            }

            // Now check to see if we're in a state where missing settings
            // requires us to use the owner's settings (we're "graph critical")
            // and load them if possible.
            Set<Long> pixelsIdsWithoutMetadata =
                getPixelsIdsWithoutMetadata(pool);
            if (pixelsIdsWithoutMetadata.size() > 0
                && securitySystem.isGraphCritical())
            {
                thumbnailList = bulkLoadOwnerMetadata(
                        dimensions, pixelsIdsWithoutMetadata); 
                for (Thumbnail metadata : thumbnailList)
                {
                    prepareMetadata(metadata, metadata.getPixels().getId());
                }
            }
        }
        s1.stop();
    }

    /**
     * Loads and prepares missing Pixels sets.
     * @param pixelsIds Pixels IDs to load missing Pixels objects for.
     */
    private void loadMissingPixels(Set<Long> pixelsIds)
    {
        if (pixelsIds.size() > 0)
        {
            Parameters parameters = new Parameters();
            parameters.addIds(pixelsIds);
            List<Pixels> pixelsWithoutSettings = queryService.findAllByQuery(
                    "select p from Pixels as p where id in (:ids)", parameters);
            for (Pixels pixels : pixelsWithoutSettings)
            {
                pixelsIdPixelsMap.put(pixels.getId(), pixels);
            }
        }
    }

    /**
     * Prepares a set of rendering settings, extracting relevant metadata and
     * preparing the internal maps.
     * @param settings RenderingDef object to prepare.
     * @param pixels Pixels object to prepare.
     */
    private void prepareRenderingSettings(RenderingDef settings, Pixels pixels)
    {
        Long pixelsId = pixels.getId();
        pixelsIdPixelsMap.put(pixelsId, pixels);
        Details details = settings.getDetails();
        Timestamp timestemp = details.getUpdateEvent().getTime();
        pixelsIdSettingsMap.put(pixelsId, settings);
        pixelsIdSettingsLastModifiedTimeMap.put(pixelsId, timestemp);
        pixelsIdSettingsOwnerIdMap.put(pixelsId, details.getOwner().getId());
    }

    /**
     * Prepares thumbnail metadata extracting relevant metadata and prepares
     * the internal maps.
     * @param metadata Thumbnail object to prepare.
     * @param pixelsId Pixels ID to prepare.
     */
    private void prepareMetadata(Thumbnail metadata, long pixelsId)
    {
        Timestamp t = metadata.getDetails().getUpdateEvent().getTime();
        pixelsIdMetadataMap.put(pixelsId, metadata);
        pixelsIdMetadataLastModifiedTimeMap.put(pixelsId, t);
    }

    /**
     * Creates missing thumbnail metadata for a set of Pixels IDs that have
     * been prepared.
     * @param dimensionPools Dimension pools to retrieve pre-calculated,
     * requested dimensions from.
     */
    private void createMissingThumbnailMetadata(
            Map<Dimension, Set<Long>> dimensionPools)
    {
        // Now check to see if we're in a state where missing metadata
        // and our state requires us to not save.
        if (securitySystem.isGraphCritical())
        {
            // TODO: Could possibly "su" to the user and create a thumbnail
            return;
        }
        StopWatch s1 = new CommonsLogStopWatch(
                "omero.createMissingThumbnailMetadata");
        List<Thumbnail> toSave = new ArrayList<Thumbnail>();
        Map<Dimension, Set<Long>> temporaryDimensionPools = 
            new HashMap<Dimension, Set<Long>>();
        Set<Long> pixelsIdsWithoutMetadata =
            getPixelsIdsWithoutMetadata(pixelsIdPixelsMap.keySet());
        for (Long pixelsId : pixelsIdsWithoutMetadata)
        {
            Pixels pixels = pixelsIdPixelsMap.get(pixelsId);
            for (Dimension dimension : dimensionPools.keySet())
            {
                Set<Long> pool = dimensionPools.get(dimension);
                if (pool.contains(pixelsId))
                {
                    toSave.add(createThumbnailMetadata(pixels, dimension));
                    addToDimensionPool(
                            temporaryDimensionPools, pixels, dimension);
                    break;
                }
            }
        }
        log.info("New thumbnail object set size: " + toSave.size());
        log.info("Dimension pool size: " + temporaryDimensionPools.size());
        if (toSave.size() > 0)
        {
            updateService.saveAndReturnIds(
                    toSave.toArray(new Thumbnail[toSave.size()]));
            loadMetadataByDimensionPool(temporaryDimensionPools);
        }
        s1.stop();
    }

    /**
     * Creates metadata for a thumbnail of a given set of pixels set and X-Y
     * dimensions.
     * 
     * @param pixels The Pixels set to create thumbnail metadata for.
     * @param dimensions The dimensions of the thumbnail.
     * 
     * @return the thumbnail metadata as created.
     * @see getThumbnailMetadata()
     */
    public Thumbnail createThumbnailMetadata(Pixels pixels,
            Dimension dimensions)
    {
        // Unload the pixels object to avoid transactional headaches
        Pixels unloadedPixels = new Pixels(pixels.getId(), false);
    
        Thumbnail thumb = new Thumbnail();
        thumb.setPixels(unloadedPixels);
        thumb.setMimeType(DEFAULT_MIME_TYPE);
        thumb.setSizeX((int) dimensions.getWidth());
        thumb.setSizeY((int) dimensions.getHeight());
        return thumb;
    }
}
