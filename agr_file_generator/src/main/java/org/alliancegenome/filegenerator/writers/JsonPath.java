package org.alliancegenome.filegenerator.writers;

import com.fasterxml.jackson.databind.JsonNode;

public final class JsonPath {

	private JsonPath() {
	}

	public static String resolveString(JsonNode root, String dottedPath) {
		JsonNode node = resolve(root, dottedPath);
		if (node == null || node.isNull() || node.isMissingNode()) {
			return "";
		}
		if (node.isValueNode()) {
			return node.asText();
		}
		return node.toString();
	}

	public static JsonNode resolve(JsonNode root, String dottedPath) {
		if (root == null || dottedPath == null || dottedPath.isEmpty()) {
			return null;
		}
		JsonNode current = root;
		for (String segment : dottedPath.split("\\.")) {
			if (current == null) {
				return null;
			}
			if (current.isArray()) {
				// Numeric segment indexes into the array.
				try {
					int idx = Integer.parseInt(segment);
					current = current.get(idx);
				} catch (NumberFormatException e) {
					return null;
				}
			} else if (current.isObject()) {
				current = current.get(segment);
			} else {
				return null;
			}
		}
		return current;
	}
}
