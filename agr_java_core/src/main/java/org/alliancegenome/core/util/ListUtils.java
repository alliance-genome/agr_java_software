package org.alliancegenome.core.util;

import java.util.ArrayList;
import java.util.List;

public final class ListUtils {

	private ListUtils() { }

	public static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> parts = new ArrayList<>();
		if (list != null) {
			for (int i = 0; i < list.size(); i += size) {
				parts.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
			}
		}
		return parts;
	}
}
