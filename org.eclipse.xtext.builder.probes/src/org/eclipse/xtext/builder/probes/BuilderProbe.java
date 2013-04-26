package org.eclipse.xtext.builder.probes;

import com.yourkit.probes.MethodPattern;
import com.yourkit.probes.Table;

public class BuilderProbe {

	private static ScheduledBuildTable T_SCHEDULED_BUILDS = new ScheduledBuildTable();

	private static class ScheduledBuildTable extends Table {
		protected ScheduledBuildTable() {
			super("Xtext Scheduled Builds", Table.MASK_FOR_POINT_EVENTS);
		}
	}

	@MethodPattern("org.eclipse.xtext.builder.impl.BuildScheduler:scheduleBuildIfNecessary(*)")
	public static class BuildSchedulerScheduleBuild {
		public static void onEnter() {
			T_SCHEDULED_BUILDS.createRow();
		}
	}
}
