/*
 * #%L
 * org.eclipse.emf.eson
 * %%
 * Copyright (C) 2013 - 2014 Michael Vorburger
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package org.eclipse.emf.eson;

import com.google.inject.Injector;
import org.eclipse.efbt.cocamo.core.model.module_management.Module_managementPackage;
import org.eclipse.efbt.cocamo.smcubes.model.core.CorePackage;
import org.eclipse.efbt.cocamo.smcubes.model.data_definition.Data_definitionPackage;
import org.eclipse.efbt.cocamo.smcubes.model.efbt_data_definition.Efbt_data_definitionPackage;
import org.eclipse.efbt.cocamo.smcubes.model.smcubes_model.Smcubes_modelPackage;
import org.eclipse.efbt.cocamo.smcubes.model.vtl_transformation.Vtl_transformationPackage;
import org.eclipse.efbt.cocamo.smcubes.model.efbt_vtl_transformation.Efbt_vtl_transformationPackage;
import org.eclipse.efbt.cocamo.core.model.functionality_module.Functionality_modulePackage;
import org.eclipse.efbt.cocamo.core.model.requirements_text.Requirements_textPackage;
import org.eclipse.emf.ecore.EPackage;



/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class EFactoryStandaloneSetup extends EFactoryStandaloneSetupGenerated {

	// TODO move this helper class into new TBD *.standalone project
	
	public static void doSetup() {
		new EFactoryStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	@Override
	public void register(Injector injector) {
	
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/module_management")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/module_management", Module_managementPackage.eINSTANCE);
	}

	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/core")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/core", CorePackage.eINSTANCE);
	}
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/data_definition")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/data_definition", Data_definitionPackage.eINSTANCE);
	}
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/efbt_data_definition")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/efbt_data_definition", Efbt_data_definitionPackage.eINSTANCE);
	}
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/smcubes_model")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/smcubes_model", Smcubes_modelPackage.eINSTANCE);
	}
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/vtl_transformation")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/vtl_transformation", Vtl_transformationPackage.eINSTANCE);
	}
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/vtl_transformation")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/vtl_transformation", Efbt_vtl_transformationPackage.eINSTANCE);
	}
	
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/functionality_module")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/functionality_module", Functionality_modulePackage.eINSTANCE);
	}
	
	if (!EPackage.Registry.INSTANCE.containsKey("http://www.eclipse.org/efbt/requirements_text")) {
		EPackage.Registry.INSTANCE.put("http://www.eclipse.org/efbt/requirements_text", Requirements_textPackage.eINSTANCE);
	}
	
	
	
	super.register(injector);
}
}
