<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<f:loadBundle basename="ome.admin.bundle.messages" var="msg"/>

<c:if test="${sessionScope.LoginBean.mode == true && sessionScope.LoginBean.role == true}">
    <f:view>
        <h:form id="experimenters">
            <h:commandLink action="#{IAEManagerBean.addNewExperimenter}">
                <h:graphicImage url="/images/add.png"/> 
                <h:outputText value=" #{msg.experimentersAddNew}" />
            </h:commandLink>
            
            <br/><br/>
            <h:message styleClass="errorText" id="experimentersError" for="experimenters"/><br /> 
            
            <h2><h:outputText value="#{msg.experimentersList}" /></h2>
            
            <h:dataTable id="items" value="#{IAEManagerBean.experimenters}" var="experimenter" styleClass="list">                
                
                <h:column>
                    <f:facet name="header">
                        <h:outputText value=" #{msg.experimentersActions} " />
                    </f:facet>
                    <h:commandLink action="#{IAEManagerBean.delExperimenter}"  onclick="if (!confirm('#{msg.experimentersConfirmation}')) return false">
                        <h:graphicImage url="/images/del.png" alt="#{msg.experimentersDelete}"/>
                    </h:commandLink>
                    <h:commandLink action="#{IAEManagerBean.editExperimenter}">
                        <h:graphicImage url="/images/edit.png" alt="#{msg.experimentersEdit}"/>
                    </h:commandLink>
                </h:column>
                
                <h:column>
                    <f:facet name="header">
                        <h:panelGroup>
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="omeName"/>
                                <f:attribute name="sort" value="asc"/>
                                <h:graphicImage url="/images/asc.png" alt="asc"/>											  						  
                            </h:commandLink>
                            
                            <h:outputText value=" #{msg.experimentersOmeName} " />		
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="omeName"/>
                                <f:attribute name="sort" value="dsc"/>
                                <h:graphicImage url="/images/dsc.png" alt="dsc"/>											  
                            </h:commandLink>
                            
                        </h:panelGroup>  
                    </f:facet>
                    
                    <h:commandLink action="#{IAEManagerBean.editExperimenter}">
                        <h:outputText value="#{experimenter.omeName}"/>
                    </h:commandLink>
                </h:column>
                
                <h:column>
                    <f:facet name="header">
                        <h:panelGroup>
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="firstName"/>
                                <f:attribute name="sort" value="asc"/>
                                <h:graphicImage url="/images/asc.png" alt="asc"/>											  						  
                            </h:commandLink>
                            
                            <h:outputText value=" #{msg.experimentersFirstName} " />		
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="firstName"/>
                                <f:attribute name="sort" value="dsc"/>
                                <h:graphicImage url="/images/dsc.png" alt="dsc"/>											  
                            </h:commandLink>
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="middleName"/>
                                <f:attribute name="sort" value="asc"/>
                                <h:graphicImage url="/images/asc.png" alt="asc"/>											  						  
                            </h:commandLink>
                            
                            <h:outputText value=" #{msg.experimentersMiddleName} " />		
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="middleName"/>
                                <f:attribute name="sort" value="dsc"/>
                                <h:graphicImage url="/images/dsc.png" alt="dsc"/>											  
                            </h:commandLink>
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="lastName"/>
                                <f:attribute name="sort" value="asc"/>
                                <h:graphicImage url="/images/asc.png" alt="asc"/>											  						  
                            </h:commandLink>
                            
                            <h:outputText value=" #{msg.experimentersLastName} " />		
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="lastName"/>
                                <f:attribute name="sort" value="dsc"/>
                                <h:graphicImage url="/images/dsc.png" alt="dsc"/>											  
                            </h:commandLink>
                            
                        </h:panelGroup>  
                        
                    </f:facet>
                    
                    <h:outputText value="#{experimenter.firstName}"/>
                    <h:outputText value=" #{experimenter.middleName}"/>
                    <h:outputText value=" #{experimenter.lastName}"/>
                </h:column>
                
                <h:column>
                    <f:facet name="header">
                        <h:panelGroup>
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="institution"/>
                                <f:attribute name="sort" value="asc"/>
                                <h:graphicImage url="/images/asc.png" alt="asc"/>											  						  
                            </h:commandLink>
                            
                            <h:outputText value=" #{msg.experimentersInstitution} " />		
                            
                            <h:commandLink styleClass="smallLink" action="sortItems" actionListener="#{IAEManagerBean.sortItems}" >
                                <f:attribute name="sortItem" value="institution"/>
                                <f:attribute name="sort" value="dsc"/>
                                <h:graphicImage url="/images/dsc.png" alt="dsc"/>											  
                            </h:commandLink>
                            
                        </h:panelGroup>  
                    </f:facet>
                    
                    <h:outputText value="#{experimenter.institution}"/>
                </h:column>
                
                
            </h:dataTable> 
        </h:form>
    </f:view>
</c:if>