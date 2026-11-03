package org.alliancegenome.indexer.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alliancegenome.curation_api.model.entities.base.AuditedObject;

import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.util.ObjectFileStorage;

@Log4j2
public class BaseService {

	protected <E> E readFromCache(String fileName, Class<E> clazz) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			File cache = new File(fileName);
			if (cache.exists()) {
				return storage.readObjectFromFile(cache);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected <E> void writeToCache(String fileName, E object) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			storage.writeObjectToFile(object, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected boolean hasNoExcludedEntities(List<AuditedObject> entitiesToBeValidated) {
		AtomicBoolean hasNoExcludedEntities = new AtomicBoolean(true);
		for (AuditedObject auditedObject : entitiesToBeValidated) {
			if (auditedObject.getObsolete()) {
				hasNoExcludedEntities.set(false);
			}
			if (auditedObject.getInternal()) {
				hasNoExcludedEntities.set(false);
			}
		}
		return hasNoExcludedEntities.get();
	}

}
