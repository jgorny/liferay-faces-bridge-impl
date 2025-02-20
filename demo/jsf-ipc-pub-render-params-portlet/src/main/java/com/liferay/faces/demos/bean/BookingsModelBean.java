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
package com.liferay.faces.demos.bean;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import com.liferay.faces.demos.dto.Booking;
import com.liferay.faces.demos.dto.Customer;
import com.liferay.faces.demos.service.CustomerService;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;


/**
 * This class is a JSF model managed-bean that contains the selected customer. First, the Portlet 2.0 EVENT_PHASE
 * broadcasts the IPC events. At that point, the bridge executes the RESTORE_VIEW phase of the JSF lifecycle so that the
 * CustomerSelectedEventHandler.handleEvent(FacesContext, Event) method can handle the "ipc.customerSelected" event as
 * defined in the WEB-INF/portlet.xml descriptor. Then, the Portlet 2.0 RENDER_PHASE will cause the RENDER_RESPONSE
 * phase of the JSF lifecycle to be executed.
 *
 * @author  Neil Griffin
 */
@RequestScoped
@ManagedBean(name = "bookingsModelBean")
public class BookingsModelBean {

	// Private Constants
	private static final Logger logger = LoggerFactory.getLogger(BookingsModelBean.class);

	// Injections
	@ManagedProperty(name = "customerService", value = "#{customerService}")
	private CustomerService customerService;

	// Private Bean Properties
	private Customer customer;
	private String selectedCustomerId;

	public Customer getCustomer() {
		return customer;
	}

	public String getSelectedCustomerId() {
		return selectedCustomerId;
	}

	@PostConstruct
	public void postConstruct() {
		logger.trace("@PostConstruct annotation worked");
		customer = new Customer();
		customer.setBookings(new ArrayList<Booking>(5));

	}

	@PreDestroy
	public void preDestroy() {
		logger.trace("@PreDestroy annotation worked");
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public void setCustomerService(CustomerService customerService) {

		// Injected via ManagedProperty annotation
		this.customerService = customerService;
	}

	/**
	 * This method will be called automatically by the bridge if the user selects a different customer from the list,
	 * because the public render parameter model-el expression #{bookingsPortlet:bookingsModelBean.selectedCustomerId}
	 * exists in the WEB-INF/faces-config.xml descriptor.
	 *
	 * @param  selectedCustomerId
	 */
	public void setSelectedCustomerId(String selectedCustomerId) {

		logger.debug("Retrieving bookings for new selectedCustomerId=[{0}]", selectedCustomerId);

		this.selectedCustomerId = selectedCustomerId;

		if (selectedCustomerId != null) {
			long customerId = Long.parseLong(selectedCustomerId);
			Customer selectedCustomer = customerService.getCustomer(customerId);
			setCustomer(selectedCustomer);
		}
	}
}
