/**
 * Copyright (c) 2000-2021 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.faces.bridge.servlet;

import java.util.Enumeration;

import javax.portlet.PortletContext;
import javax.portlet.faces.BridgeFactoryFinder;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.liferay.faces.bridge.bean.BeanManager;
import com.liferay.faces.bridge.bean.BeanManagerFactory;
import com.liferay.faces.bridge.bean.PreDestroyInvoker;
import com.liferay.faces.bridge.bean.PreDestroyInvokerFactory;
import com.liferay.faces.bridge.context.internal.PortletContextAdapter;
import com.liferay.faces.bridge.scope.internal.BridgeRequestScopeManager;
import com.liferay.faces.bridge.scope.internal.BridgeRequestScopeManagerFactory;
import com.liferay.faces.util.config.ApplicationConfig;
import com.liferay.faces.util.lang.ThreadSafeAccessor;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;
import com.liferay.faces.util.product.Product;
import com.liferay.faces.util.product.ProductFactory;


/**
 * This class provides the ability to cleanup session-scoped and view-scoped managed-beans upon session expiration.
 *
 * @author  Neil Griffin
 */
public class BridgeSessionListener implements HttpSessionListener, ServletContextListener {

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(BridgeSessionListener.class);

	// Private Constants
	private static final String MOJARRA_ACTIVE_VIEW_MAPS = "com.sun.faces.application.view.activeViewMaps";
	private static final String MOJARRA_PACKAGE_PREFIX = "com.sun.faces";
	private static final String MOJARRA_VIEW_SCOPE_MANAGER = "com.sun.faces.application.view.viewScopeManager";

	// Private Final Data Members
	private final MojarraAbleToCleanUpViewScopedDataAccessor mojarraAbleToCleanUpViewScopedDataAccessor =
		new MojarraAbleToCleanUpViewScopedDataAccessor();

	// Private Data Members
	private boolean firstInstance;

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		ServletContext servletContext = servletContextEvent.getServletContext();
		PortletContext portletContext = new PortletContextAdapter(servletContext);
		BridgeFactoryFinder.getInstance().releaseFactories(portletContext);
	}

	/**
	 * This method provides the ability to discover the Mojarra InjectionProvider at startup.
	 */
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {

		ServletContext servletContext = servletContextEvent.getServletContext();

		if (servletContext.getAttribute(BridgeSessionListener.class.getName()) == null) {

			logger.info("Context initialized for contextPath=[{0}]", servletContext.getContextPath());

			// Prevent multiple-instantiation of this listener.
			servletContext.setAttribute(BridgeSessionListener.class.getName(), Boolean.TRUE);
			firstInstance = true;

		}
		else {
			logger.debug("Preventing multiple instantiation for contextPath=[{0}]", servletContext.getContextPath());
		}
	}

	@Override
	public void sessionCreated(HttpSessionEvent httpSessionEvent) {

		// FACES-2427: Prevent an error message during session expiration by ensuring that the BridgeFactoryFinder has
		// been initialized during session creation.
		HttpSession httpSession = httpSessionEvent.getSession();
		ServletContext servletContext = httpSession.getServletContext();
		PortletContext portletContext = new PortletContextAdapter(servletContext);
		BridgeFactoryFinder.getFactory(portletContext, BeanManagerFactory.class);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {

		if (firstInstance) {

			// Discover Factories
			BeanManagerFactory beanManagerFactory = null;
			BridgeRequestScopeManagerFactory bridgeRequestScopeManagerFactory = null;
			HttpSession httpSession = null;
			ServletContext servletContext = null;
			PortletContext portletContext = null;

			try {

				httpSession = httpSessionEvent.getSession();
				servletContext = httpSession.getServletContext();
				portletContext = new PortletContextAdapter(servletContext);
				beanManagerFactory = (BeanManagerFactory) BridgeFactoryFinder.getFactory(portletContext,
						BeanManagerFactory.class);
				bridgeRequestScopeManagerFactory = (BridgeRequestScopeManagerFactory) BridgeFactoryFinder.getFactory(
						portletContext, BridgeRequestScopeManagerFactory.class);
			}
			catch (Exception e) {

				String contextPath = "unknown";

				if (servletContext != null) {
					contextPath = servletContext.getContextPath();
				}

				logger.error(
					"Unable to discover factories for contextPath=[{0}] possibly because the portlet never received a RenderRequest",
					contextPath);
			}

			if ((beanManagerFactory != null) && (bridgeRequestScopeManagerFactory != null)) {

				// Cleanup instances of BridgeRequestScope that are associated with the expiring session.
				BridgeRequestScopeManager bridgeRequestScopeManager =
					bridgeRequestScopeManagerFactory.getBridgeRequestScopeManager(portletContext);
				bridgeRequestScopeManager.removeBridgeRequestScopesBySession(httpSession);

				// For each session attribute:
				String appConfigAttrName = ApplicationConfig.class.getName();
				ApplicationConfig applicationConfig = (ApplicationConfig) servletContext.getAttribute(
						appConfigAttrName);
				BeanManager beanManager = beanManagerFactory.getBeanManager(applicationConfig.getFacesConfig());

				try {

					Enumeration<String> attributeNames = httpSession.getAttributeNames();

					while (attributeNames.hasMoreElements()) {

						String attributeName = attributeNames.nextElement();

						// If the current session attribute name is namespaced with the standard portlet prefix, then it
						// is an attribute that was set using PortletSession.setAttribute(String, Object).
						if ((attributeName != null) && attributeName.startsWith("javax.portlet.p.")) {
							int pos = attributeName.indexOf("?");

							if (pos > 0) {
								Object attributeValue = httpSession.getAttribute(attributeName);
								httpSession.removeAttribute(attributeName);

								if (attributeValue != null) {

									// If the current session attribute value is a JSF managed-bean, then cleanup the
									// bean by invoking methods annotated with {@link PreDestroy}. Note that in a
									// webapp/servlet environment, the cleanup is handled by the Mojarra
									// WebappLifecycleListener.sessionDestroyed(HttpSessionEvent) method. But in a
									// portlet environment, Mojarra fails to recognize the session attribute as
									// managed-bean because the attribute name contains the standard portlet prefix. An
									// alternative approach would be to have the bridge rename the attribute (by
									// stripping off the standard portlet prefix) so that Mojarra could find it. But
									// this would not a good solution, because multiple instances of the same portlet
									// would have the same session attribute names for managed-beans, and only the last
									// one would get cleaned-up by Mojarra.
									if (beanManager.isManagedBean(attributeName, attributeValue)) {

										PreDestroyInvokerFactory preDestroyInvokerFactory = (PreDestroyInvokerFactory)
											BridgeFactoryFinder.getFactory(portletContext,
												PreDestroyInvokerFactory.class);
										PreDestroyInvoker preDestroyInvoker =
											preDestroyInvokerFactory.getPreDestroyInvoker(servletContext);
										preDestroyInvoker.invokeAnnotatedMethods(attributeValue, true);
									}

									// Otherwise,
									else {

										// If the current session attribute is Mojarra-vendor-specific, then
										String attributeValueClassName = attributeValue.getClass().getName();

										if (attributeName.contains(MOJARRA_ACTIVE_VIEW_MAPS) ||
												attributeValueClassName.contains(MOJARRA_PACKAGE_PREFIX)) {

											// Rename the namespaced attribute by stripping off the standard portlet
											// prefix. This will enable Mojarra's session expiration features to find
											// attributes that it is expecting.
											String nonPrefixedName = attributeName.substring(pos + 1);
											logger.debug("Renaming Mojarra session attributeName=[{0}] -> [{1}]",
												attributeName, nonPrefixedName);
											httpSession.setAttribute(nonPrefixedName, attributeValue);

											// If this is the attribute that contains all of the active view maps, then
											if (MOJARRA_ACTIVE_VIEW_MAPS.equals(nonPrefixedName)) {

												if (mojarraAbleToCleanUpViewScopedDataAccessor.get(portletContext)) {

													// Invoke the Mojarra
													// ViewScopeManager.sessionDestroyed(HttpSessionEvent) method in
													// order to cleanup the active view maps. Rather than waiting for
													// the servlet container to call the method during session
													// expiration, it is important to call it directly within this
													// while-loop for two reasons: 1) If the developer did not
													// explicitly specify the order of the Mojarra ConfigureListener and
													// this BridgeSessionListener in WEB-IN/web.xml descriptor
													// (FACES-1483) then there is no guarantee that the method would get
													// called. 2) In the case of multiple portlet instances, each
													// instance has its own namespaced attribute in the session.
													// Renaming each namespaced attribute to
													// "com.sun.faces.application.view.activeViewMaps" would only enable
													// Mojarra to cleanup the last one.
													HttpSessionListener viewScopeManager = (HttpSessionListener)
														servletContext.getAttribute(MOJARRA_VIEW_SCOPE_MANAGER);

													if (viewScopeManager != null) {

														try {
															logger.debug(
																"Asking Mojarra ViewScopeManager to cleanup @ViewScoped managed-beans");
															viewScopeManager.sessionDestroyed(httpSessionEvent);
														}
														catch (Exception e) {
															logger.error(e);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				catch (IllegalStateException e) {
					logger.warn("Server does not permit cleanup of Mojarra managed-beans during session expiration");
				}
			}
		}
	}

	private static final class MojarraAbleToCleanUpViewScopedDataAccessor
		extends ThreadSafeAccessor<Boolean, PortletContext> {

		@Override
		protected Boolean computeValue(PortletContext portletContext) {

			// Determine if Mojarra is able to cleanup the active view maps.
			ProductFactory productFactory = (ProductFactory) BridgeFactoryFinder.getFactory(portletContext,
					ProductFactory.class);
			Product mojarra = productFactory.getProductInfo(Product.Name.MOJARRA);
			boolean mojarraAbleToCleanup = true;

			if (mojarra.isDetected() && (mojarra.getMajorVersion() == 2) && (mojarra.getMinorVersion() == 1)) {

				if (mojarra.getPatchVersion() < 18) {
					mojarraAbleToCleanup = false;

					boolean logWarning = true;
					Product iceFaces = productFactory.getProductInfo(Product.Name.ICEFACES);

					if (iceFaces.isDetected()) {

						if ((iceFaces.getMajorVersion() == 2) ||
								((iceFaces.getMajorVersion() == 3) && (iceFaces.getMinorVersion() <= 2))) {

							// Versions of ICEfaces prior to 3.3 can only go as high as Mojarra 2.1.6 so don't bother to
							// log the warning.
							logWarning = false;
						}
					}

					if (logWarning) {
						logger.warn("Unable to cleanup ViewScoped managed-beans upon session expiration. " +
							"Please upgrade to Mojarra 2.1.18 or newer. " +
							"For more info, see: http://issues.liferay.com/browse/FACES-1470");
					}
				}
			}

			return mojarraAbleToCleanup;
		}
	}
}
