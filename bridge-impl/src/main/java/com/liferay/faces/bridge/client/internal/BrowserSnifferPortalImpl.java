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
package com.liferay.faces.bridge.client.internal;

import com.liferay.faces.util.client.BrowserSniffer;


/**
 * @author  Kyle Stiemann
 */
public class BrowserSnifferPortalImpl implements BrowserSniffer {

	@Override
	public boolean acceptsGzip() {
		return false;
	}

	@Override
	public String getBrowserId() {
		return "";
	}

	@Override
	public float getMajorVersion() {
		return 0;
	}

	@Override
	public String getRevision() {
		return "";
	}

	@Override
	public String getVersion() {
		return "";
	}

	@Override
	public boolean isAir() {
		return false;
	}

	@Override
	public boolean isAndroid() {
		return false;
	}

	@Override
	public boolean isChrome() {
		return false;
	}

	@Override
	public boolean isFirefox() {
		return false;
	}

	@Override
	public boolean isGecko() {
		return false;
	}

	@Override
	public boolean isIe() {
		return false;
	}

	@Override
	public boolean isIeOnWin32() {
		return false;
	}

	@Override
	public boolean isIeOnWin64() {
		return false;
	}

	@Override
	public boolean isIpad() {
		return false;
	}

	@Override
	public boolean isIphone() {
		return false;
	}

	@Override
	public boolean isLinux() {
		return false;
	}

	@Override
	public boolean isMac() {
		return false;
	}

	@Override
	public boolean isMobile() {
		return false;
	}

	@Override
	public boolean isMozilla() {
		return false;
	}

	@Override
	public boolean isOpera() {
		return false;
	}

	@Override
	public boolean isRtf() {
		return false;
	}

	@Override
	public boolean isSafari() {
		return false;
	}

	@Override
	public boolean isSun() {
		return false;
	}

	@Override
	public boolean isWebKit() {
		return false;
	}

	@Override
	public boolean isWindows() {
		return false;
	}
}
