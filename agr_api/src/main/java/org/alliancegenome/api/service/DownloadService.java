package org.alliancegenome.api.service;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alliancegenome.api.dto.DownloadFile;
import org.alliancegenome.api.entity.ReleaseInfoDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@RequestScoped
@Slf4j
public class DownloadService {

	private static final String DOWNLOAD_HOST = "https://download.alliancegenome.org";

	@Inject ReleaseInfoService releaseInfoService;

	public String getCurrentRelease() {
		ReleaseInfoDocument doc = releaseInfoService.getReleaseInfo();
		if (doc == null || doc.getReleaseVersion() == null || doc.getReleaseVersion().isEmpty()) {
			throw new IllegalStateException("Could not resolve current release version from Elasticsearch (releaseInfo document missing).");
		}
		return doc.getReleaseVersion();
	}

	public InputStream openDownloadStream(String release, String filename) throws Exception {
		String url = DOWNLOAD_HOST + "/" + release + "/downloads/" + filename;
		log.info("Streaming download: {}", url);
		return new URL(url).openStream();
	}

	public List<DownloadFile> listDownloads(String release) throws Exception {
		String prefix = release + "/downloads/";
		String url = DOWNLOAD_HOST + "/?list-type=2&prefix=" + URLEncoder.encode(prefix, "UTF-8") + "&delimiter=%2F&max-keys=1000";
		log.info("Listing downloads: {}", url);

		List<DownloadFile> out = new ArrayList<>();
		try (InputStream is = new URL(url).openStream()) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			NodeList contents = doc.getElementsByTagName("Contents");
			for (int i = 0; i < contents.getLength(); i++) {
				Element c = (Element) contents.item(i);
				String key = textOf(c, "Key");
				if (key == null || key.endsWith("/")) {
					continue;
				}
				String filename = key.substring(key.lastIndexOf('/') + 1);
				DownloadFile f = new DownloadFile();
				f.setFilename(filename);
				f.setS3Path(key);
				f.setS3Url(DOWNLOAD_HOST + "/" + key);
				f.setStableURL("/download/" + filename);
				f.setReleaseVersion(release);
				f.setLastModified(textOf(c, "LastModified"));
				String size = textOf(c, "Size");
				f.setSize(size == null ? 0L : Long.parseLong(size));
				populateTypeFields(f, filename);
				out.add(f);
			}
		}
		return out;
	}

	private static String textOf(Element parent, String tag) {
		NodeList nl = parent.getElementsByTagName(tag);
		if (nl.getLength() == 0) {
			return null;
		}
		return nl.item(0).getTextContent();
	}

	private static void populateTypeFields(DownloadFile f, String filename) {
		String name = filename;
		String ext = "";
		int dot = name.indexOf('.');
		if (dot > 0) {
			ext = name.substring(dot + 1);
			name = name.substring(0, dot);
		}
		f.setFileExtension(ext);
		int us = name.lastIndexOf('_');
		if (us > 0) {
			f.setDataType(name.substring(0, us));
			f.setDataSubType(name.substring(us + 1));
		} else {
			f.setDataType(name);
		}
	}

}
