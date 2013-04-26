package org.eclipse.ui.workbench.probes;

import org.eclipse.ui.IEditorInput;

import com.yourkit.probes.MethodPattern;
import com.yourkit.probes.OnEnterResult;
import com.yourkit.probes.Param;
import com.yourkit.probes.StringColumn;
import com.yourkit.probes.Table;
import com.yourkit.probes.This;

public class OpenEditorProbe {

	private static class OpenEditorTable extends Table {
		private final StringColumn EDITOR_ID = new StringColumn("EditorID");
		private final StringColumn EDITOR_INPUT = new StringColumn("EditorInput");

		protected OpenEditorTable() {
			super("Eclipse Open Editor", Table.MASK_FOR_SINGLE_METHOD_CALL_LASTING_EVENTS | Table.RECORD_THREAD_CPU_TIME);
		}
	}

	private static OpenEditorTable T_OPEN_EDITOR = new OpenEditorTable();

	@MethodPattern("org.eclipse.ui.internal.WorkbenchPage:openEditor(org.eclipse.ui.IEditorInput, java.lang.String, boolean, int, org.eclipse.ui.IMemento, boolean)")
	public static class WorkbenchPageOpenEditor {

		public static int onEnter(@This Object workbenchPage, @Param(1) IEditorInput input, @Param(2) String editorID) {
			int row = T_OPEN_EDITOR.createRow();
			T_OPEN_EDITOR.EDITOR_ID.setValue(row, editorID);
			T_OPEN_EDITOR.EDITOR_INPUT.setValue(row, input == null ? "null" : input.toString());
			return row;
		}

		public static void onReturn(@OnEnterResult final int readRowIndex) {
			if (readRowIndex >= 0)
				T_OPEN_EDITOR.closeRow(readRowIndex);
		}

	}
}
