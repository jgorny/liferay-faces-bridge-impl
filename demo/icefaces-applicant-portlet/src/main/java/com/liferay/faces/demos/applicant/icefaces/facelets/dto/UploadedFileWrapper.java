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
package com.liferay.faces.demos.applicant.icefaces.facelets.dto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.faces.FacesWrapper;

import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;
import org.icefaces.ace.component.fileentry.FileEntryStatus;

import com.liferay.faces.bridge.model.UploadedFile;


/**
 * This class provides a convenient mechanism for converting an ICEfaces {@link FileInfo} object to an instance of the
 * {@link UploadedFile} interface provided by the Liferay Faces Bridge implementation.
 *
 * @author  Neil Griffin
 */
public class UploadedFileWrapper implements Serializable, UploadedFile, FacesWrapper<FileInfo> {

	// serialVersionUID
	private static final long serialVersionUID = 5356309286451276753L;

	// Private Data Members
	Map<String, Object> attributeMap;
	private String id;
	private UploadedFile.Status status;
	private FileInfo wrappedFileInfo;

	public UploadedFileWrapper(FileInfo fileInfo) {
		this.wrappedFileInfo = fileInfo;
		this.attributeMap = new HashMap<String, Object>();
		this.id = Long.toString(((long) hashCode()) + System.currentTimeMillis());

		FileEntryStatus fileEntryStatus = wrappedFileInfo.getStatus();

		if (fileEntryStatus.isSuccess()) {
			status = Status.FILE_SAVED;
		}
		else {
			status = Status.ERROR;
		}
	}

	public void delete() throws IOException {
		wrappedFileInfo.getFile().delete();
	}

	public String getAbsolutePath() {
		return wrappedFileInfo.getFile().getAbsolutePath();
	}

	public Map<String, Object> getAttributes() {
		return attributeMap;
	}

	public byte[] getBytes() throws IOException {
		byte[] bytes = null;

		try {
			File file = wrappedFileInfo.getFile();

			if (file.exists()) {
				RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
				bytes = new byte[(int) randomAccessFile.length()];
				randomAccessFile.readFully(bytes);
				randomAccessFile.close();
			}
		}
		catch (Exception e) {
			throw new IOException(e.getMessage());
		}

		return bytes;
	}

	public String getCharSet() {
		throw new UnsupportedOperationException();
	}

	public String getContentType() {
		return wrappedFileInfo.getContentType();
	}

	public String getHeader(String name) {
		throw new UnsupportedOperationException();
	}

	public Collection<String> getHeaderNames() {
		throw new UnsupportedOperationException();
	}

	public Collection<String> getHeaders(String name) {
		throw new UnsupportedOperationException();
	}

	public String getId() {
		return id;
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(wrappedFileInfo.getFile());
	}

	public String getMessage() {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		return wrappedFileInfo.getFileName();
	}

	public long getSize() {
		return wrappedFileInfo.getSize();
	}

	public Status getStatus() {
		return status;
	}

	public FileInfo getWrapped() {
		return wrappedFileInfo;
	}

	public void write(String fileName) throws IOException {
		OutputStream outputStream = new FileOutputStream(fileName);
		outputStream.write(getBytes());
		outputStream.close();
	}
}
