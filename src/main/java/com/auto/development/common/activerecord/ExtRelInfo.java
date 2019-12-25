package com.auto.development.common.activerecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-05 22:47
 */
@Data
@Accessors(chain = true)
public class ExtRelInfo<T extends XModel<?>> {

	private String midTable;

	private String midEntry;

	private String midProperty;

	private String relColumn;

	private String relUri;

	private String relTable;

	private String relTablePk;

	private String relEntry;

	private String relProperty;

	private String relToTable;

	private String relToColumn;

	private String relToUri;

	private String relToEntry;

	private String relToProperty;

	private String relToTablePk;

	private boolean isMany;

	private boolean injectBeRelEntry = true;

	private XModel model;

	private Field declaredField;

	private boolean isReachable;

	private Class<T> relEntryClass;

	private Class<T> relToEntryClass;

	private Class<T> midEntryClass;

	private Class<T> fieldActualClass;

	public boolean getIsMany() {
		return isMany;
	}

	public boolean getInjectBeRelEntry() {
		return injectBeRelEntry;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExtRelInfo that = (ExtRelInfo) o;
		return Objects.equals(relColumn, that.relColumn) &&
				Objects.equals(relToColumn, that.relToColumn) &&
				Objects.equals(midTable, that.midTable) &&
				Objects.equals(relToTable, that.relToTable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(relColumn, relToColumn, midTable, relToTable);
	}
}
