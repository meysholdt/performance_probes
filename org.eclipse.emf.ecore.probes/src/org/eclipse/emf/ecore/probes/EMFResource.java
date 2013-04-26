package org.eclipse.emf.ecore.probes;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;

import com.yourkit.probes.ForeignKeyColumn;
import com.yourkit.probes.JVM;
import com.yourkit.probes.MethodPattern;
import com.yourkit.probes.ObjectRowIndexMap;
import com.yourkit.probes.OnEnterResult;
import com.yourkit.probes.Param;
import com.yourkit.probes.StringColumn;
import com.yourkit.probes.Table;
import com.yourkit.probes.This;

public class EMFResource {

	private final static ResourceSetTable T_RESOURCESET = new ResourceSetTable();

	private final static int T_RESOURCESET_NO_RS = T_RESOURCESET.createRow();

	private static final class ResourceSetTable extends Table {
		private final StringColumn resourceSet = new StringColumn("ResourceSet");
		private final StringColumn context = new StringColumn("XtextClasspathContext");

		public ResourceSetTable() {
			super("EMF ResourceSets", Table.MASK_FOR_POINT_EVENTS);
		}
	}

	private static final ResourceTable T_RESOURCE = new ResourceTable();

	private static final class ResourceTable extends Table {
		private final ForeignKeyColumn rs = new ForeignKeyColumn(T_RESOURCESET);
		private final StringColumn instance = new StringColumn("instance");
		private final StringColumn type = new StringColumn("type");
		private final StringColumn uri = new StringColumn("uri");
		private final StringColumn op = new StringColumn("operation");

		public ResourceTable() {
			super("EMF Resources", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}
	}

	private final static ObjectRowIndexMap<ResourceSet> rs2rwo = new ObjectRowIndexMap<ResourceSet>();

	@MethodPattern("org.eclipse.emf.ecore.resource.impl.ResourceSetImpl:<init>()")
	public static class ResourceSetInit {
		public static void onEnter(@This final ResourceSet rs) {
			synchronized (rs) {
				getResourceSetRwo(rs);
			}
		}
	}

	@MethodPattern("org.eclipse.xtext.resource.XtextResourceSet:setClasspathURIContext(java.lang.Object)")
	public static class ResourceSetSetClasspathContext {
		public static void onEnter(@This final ResourceSet rs, @Param(1) Object ctx) {
			synchronized (rs) {
				int i = getResourceSetRwo(rs);
				T_RESOURCESET.context.setValue(i, ctxToStr(ctx));
			}
		}

		private static String ctxToStr(Object ctx) {
			if (ctx instanceof IJavaProject)
				return ctx.getClass().getSimpleName() + "(" + ((IJavaProject) ctx).getProject().getName() + ")";
			if (ctx != null)
				return ctx.getClass().getSimpleName();
			return "null";
		}
	}

	// @MethodPattern("org.eclipse.emf.ecore.resource.impl.ResourceImpl:basicSetResourceSet(*)")
	// public static class ResourceBasicSetResourceSetInit {
	// public static void onEnter(@This final Resource r,
	// @Param(1) ResourceSet rs) {
	// int rsRow = getResourceSetRwo(rs);
	// int rRow = T_RESOURCE.createRow();
	// T_RESOURCE.rs.setValue(rRow, rsRow);
	// T_RESOURCE.op.setValue(rRow, r.getClass().getSimpleName()
	// + "<init>(" + resourceSetToStr(rs) + ")");
	// }
	// }

	private static int getResourceSetRwo(ResourceSet rs) {
		if (rs == null)
			return T_RESOURCESET_NO_RS;
		synchronized (rs) {
			int i = rs2rwo.get(rs);
			if (i == Table.NO_ROW) {
				i = T_RESOURCESET.createRow();
				T_RESOURCESET.resourceSet.setValue(i, resourceSetToStr(rs));
				rs2rwo.put(rs, i);
			}
			return i;
		}
	}

	private static String resourceSetToStr(ResourceSet rs) {
		return rs.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(rs));
	}

	@MethodPattern("org.eclipse.emf.ecore.resource.impl.ResourceImpl:load(java.io.InputStream, java.util.Map)")
	public static class ResourceLoad {

		public static int onEnter(@This final Resource resource) {
			if (!JVM.isLivePhase()) {
				return -1;
			}
			synchronized (resource) {
				int rsRow = getResourceSetRwo(resource.getResourceSet());
				int rRow = T_RESOURCE.createRow();
				T_RESOURCE.rs.setValue(rRow, rsRow);
				T_RESOURCE.instance.setValue(rRow, Integer.toHexString(System.identityHashCode(resource)));
				T_RESOURCE.type.setValue(rRow, resource.getClass().getSimpleName());
				T_RESOURCE.uri.setValue(rRow, resource.getURI().toString());
				T_RESOURCE.op.setValue(rRow, "load()");
				return rRow;
			}
		}

		public static void onReturn(@OnEnterResult final int readRowIndex) {
			if (readRowIndex >= 0)
				T_RESOURCE.closeRow(readRowIndex);
		}

	}
}
