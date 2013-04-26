package org.eclipse.emf.ecore.probes;

import com.yourkit.probes.Probes;

public class Startup implements org.eclipse.ui.IStartup {

	@Override
	public void earlyStartup() {
		System.out.println("registering probes...");
		Probes.registerProbes(EMFResource.class);
		System.out.println("done");
	}

}
