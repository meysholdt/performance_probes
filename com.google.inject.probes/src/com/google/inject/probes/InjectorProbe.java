package com.google.inject.probes;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yourkit.probes.ForeignKeyColumn;
import com.yourkit.probes.MethodPattern;
import com.yourkit.probes.ObjectRowIndexMap;
import com.yourkit.probes.OnEnterResult;
import com.yourkit.probes.ReturnValue;
import com.yourkit.probes.StringColumn;
import com.yourkit.probes.Table;
import com.yourkit.probes.This;

public class InjectorProbe {

	private static final InjectorTable TABLE_INJECTOR = new InjectorTable();

	private static class InjectorTable extends Table {
		private final StringColumn HASHCODE = new StringColumn("HashCode");
		private final StringColumn XTEXT_LANG = new StringColumn("Xtext Lang");

		protected InjectorTable() {
			super("Guice Injectors", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}
	}

	private static class InstanceTable extends Table {
		@SuppressWarnings("unused")
		private final ForeignKeyColumn INJECTOR = new ForeignKeyColumn(TABLE_INJECTOR);
		private final StringColumn TYPE = new StringColumn("Type");
		private final StringColumn HASHCODE = new StringColumn("HashCode");

		protected InstanceTable() {
			super("Instances", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}

	}

	private static final InstanceTable TABLE_INSTANCE = new InstanceTable();

	@MethodPattern("com.google.inject.internal.InternalInjectorCreator:build()")
	public static class InjectorCreation {

		public static int onEnter() {
			int row = TABLE_INJECTOR.createRow();
			thread2row.push(row);
			// System.out.println(Thread.currentThread().getName() +
			// "->thread2row.set(" + row + ");");
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex, @ReturnValue Injector inj) {
			if (readRowIndex >= 0) {
				injector2row.put(inj, readRowIndex);
				TABLE_INJECTOR.HASHCODE.setValue(readRowIndex, Integer.toHexString(System.identityHashCode(inj)));
				TABLE_INJECTOR.XTEXT_LANG.setValue(readRowIndex, getInjectorName(inj));
				TABLE_INJECTOR.closeRow(readRowIndex);
			}
			thread2row.pop();
			// System.out.println(Thread.currentThread().getName() +
			// "->thread2row.set(null); " + getInjectorName(inj));
		}
	}

	private static final ObjectRowIndexMap<Injector> injector2row = new ObjectRowIndexMap<Injector>();
	private static final ThreadLocalStack<Integer> thread2row = new ThreadLocalStack<Integer>();

	private static int getInjectorRow(Injector inj) {
		int i = injector2row.get(inj);
		if (i != Table.NO_ROW)
			return i;
		Integer x = thread2row.peek();
		if (x != null)
			return x;
		System.out.println("row in thread " + Thread.currentThread().getName());
		return Table.NO_ROW;
	}

	@MethodPattern("com.google.inject.internal.ConstructorInjector:construct(com.google.inject.internal.Errors, com.google.inject.internal.InternalContext, java.lang.Class, boolean)")
	public static class ConstructorInjectorConstruct {

		public static int onEnter(@This Object constructorInjector) {
			Object membersInjector = ReflectionUtil.readField(constructorInjector, "membersInjector", Object.class);
			Injector injector = ReflectionUtil.readField(membersInjector, "injector", Injector.class);
			int row = TABLE_INSTANCE.createRow(getInjectorRow(injector));
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex, @ReturnValue Object object) {
			if (readRowIndex >= 0) {
				TABLE_INSTANCE.TYPE.setValue(readRowIndex, object == null ? "void" : object.getClass().getName());
				TABLE_INSTANCE.HASHCODE.setValue(readRowIndex, Integer.toHexString(System.identityHashCode(object)));
				TABLE_INSTANCE.closeRow(readRowIndex);
			}
		}

	}

	private static String getInjectorName(Injector inj) {
		try {
			String name = inj.getInstance(Key.get(String.class, Names.named("languageName")));
			if (name == null)
				return "(null)";
			int i = name.lastIndexOf('.');
			if (i >= 0)
				return name.substring(i + 1);
			return name;
		} catch (Throwable t) {
			return t.getMessage();
		}
	}
}
