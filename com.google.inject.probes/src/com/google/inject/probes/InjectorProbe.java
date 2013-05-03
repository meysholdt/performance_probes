package com.google.inject.probes;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yourkit.probes.ForeignKeyColumn;
import com.yourkit.probes.MethodPattern;
import com.yourkit.probes.ObjectRowIndexMap;
import com.yourkit.probes.OnEnterResult;
import com.yourkit.probes.Param;
import com.yourkit.probes.ReturnValue;
import com.yourkit.probes.StringColumn;
import com.yourkit.probes.Table;
import com.yourkit.probes.This;
import com.yourkit.probes.ThrownException;

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
		private final StringColumn PARENT = new StringColumn("Parent");
		private final StringColumn TYPE = new StringColumn("Type");
		private final StringColumn HASHCODE = new StringColumn("HashCode");

		protected InstanceTable() {
			super("Instances", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}
	}

	private static class ModuleConfigureTable extends Table {

		@SuppressWarnings("unused")
		private final ForeignKeyColumn INJECTOR = new ForeignKeyColumn(TABLE_INJECTOR);
		private final StringColumn TYPE = new StringColumn("Type");

		protected ModuleConfigureTable() {
			super("XtextModule.configure()", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}
	}

	private static class JITBindingTable extends Table {
		private final ForeignKeyColumn INJECTOR = new ForeignKeyColumn(TABLE_INJECTOR);
		private final StringColumn KEY = new StringColumn("Key");
		private final StringColumn PARENT = new StringColumn("Parent");
		private final StringColumn RESULT = new StringColumn("Result");

		protected JITBindingTable() {
			super("JIT Bindings", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}

	}

	private static final ModuleConfigureTable TABLE_MODULE = new ModuleConfigureTable();

	private static final InstanceTable TABLE_INSTANCE = new InstanceTable();
	private static final JITBindingTable TABLE_JIT = new JITBindingTable();

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
	private static final ThreadLocalStack<Integer> obj2row = new ThreadLocalStack<Integer>();

	private static int getInjectorRow(Injector inj) {
		if (inj != null) {
			int i = injector2row.get(inj);
			if (i != Table.NO_ROW)
				return i;
		}
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
			Integer parent = obj2row.peek();
			if (parent != null)
				TABLE_INSTANCE.PARENT.setValue(row, "#" + parent);
			obj2row.push(row);
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex, @ReturnValue Object object) {
			if (readRowIndex >= 0) {
				obj2row.pop();
				TABLE_INSTANCE.TYPE.setValue(readRowIndex, object == null ? "void" : object.getClass().getName());
				TABLE_INSTANCE.HASHCODE.setValue(readRowIndex, Integer.toHexString(System.identityHashCode(object)));
				TABLE_INSTANCE.closeRow(readRowIndex);
			}
		}

	}

	@MethodPattern("org.eclipse.xtext.service.AbstractGenericModule:configure(com.google.inject.Binder)")
	public static class XtextModuleConfigure {

		public static int onEnter(@This Object module) {
			int row = TABLE_MODULE.createRow(getInjectorRow(null));
			TABLE_MODULE.TYPE.setValue(row, module.getClass().getName());
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex, @ReturnValue Object object) {
			if (readRowIndex >= 0) {
				TABLE_MODULE.closeRow(readRowIndex);
			}
		}
	}

	private static ThreadLocalStack<Integer> jitBindings = new ThreadLocalStack<Integer>();

	@MethodPattern("com.google.inject.internal.InjectorImpl:createJustInTimeBindingRecursive(com.google.inject.Key, com.google.inject.internal.Errors, boolean, com.google.inject.internal.InjectorImpl$JitLimitation)")
	public static class InjectorJITBinding {

		public static int onEnter(@This Injector injector, @Param(1) Object key) {
			System.out.println("jit");
			int row = TABLE_JIT.createRow(getInjectorRow(injector));
			TABLE_JIT.KEY.setValue(row, key.toString());
			Integer parent = jitBindings.peek();
			if (parent != null)
				TABLE_JIT.PARENT.setValue(row, "#" + parent);
			jitBindings.push(row);
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex) {
			if (readRowIndex >= 0) {
				TABLE_JIT.RESULT.setValue(readRowIndex, "ok");
				jitBindings.pop();
				TABLE_JIT.closeRow(readRowIndex);
			}
		}

		public static void onUncaughtException(@OnEnterResult final int readRowIndex, @ThrownException Throwable exception) {
			if (readRowIndex >= 0) {
				TABLE_JIT.RESULT.setValue(readRowIndex, exception.toString());
				jitBindings.pop();
				TABLE_JIT.closeRow(readRowIndex);
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
