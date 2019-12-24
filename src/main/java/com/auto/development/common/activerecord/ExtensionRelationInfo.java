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
public class ExtensionRelationInfo<T extends XModel<?>> {

	private String middleTable;

	private String middleEntry;

	private String middleProperty;

	private String relationColumn;

	private String relationUri;

	private String relationTable;

	private String relationTablePk;

	private String relationEntry;

	private String relationProperty;

	private String relationToTable;

	private String relationToColumn;

	private String relationToUri;

	private String relationToEntry;

	private String relationToProperty;

	private String relationToTablePk;

	private boolean isMany;

	private boolean injectBeRelationEntry = true;

	private XModel model;

	private Field declaredField;

	private boolean isReachable;

	private Class<T> relationEntryClass;

	private Class<T> relationToEntryClass;

	private Class<T> middleEntryClass;

	private Class<T> fieldActualClass;

	public boolean getIsMany() {
		return isMany;
	}

	public boolean getInjectBeRelationEntry() {
		return injectBeRelationEntry;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExtensionRelationInfo that = (ExtensionRelationInfo) o;
		return Objects.equals(relationColumn, that.relationColumn) &&
				Objects.equals(relationToColumn, that.relationToColumn) &&
				Objects.equals(middleTable, that.middleTable) &&
				Objects.equals(relationToTable, that.relationToTable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(relationColumn, relationToColumn, middleTable, relationToTable);
	}
}
