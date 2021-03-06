/*
 * #%L
 * org.eclipse.emf.eson
 * %%
 * Copyright (C) 2009 - 2014 Sebastian Benz, Michael Vorburger and others.
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package org.eclipse.emf.eson.validation;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.eson.building.NameAccessor;
import org.eclipse.emf.eson.building.NewObjectExtensions;
import org.eclipse.emf.eson.eFactory.Attribute;
import org.eclipse.emf.eson.eFactory.BooleanAttribute;
import org.eclipse.emf.eson.eFactory.Containment;
import org.eclipse.emf.eson.eFactory.DoubleAttribute;
import org.eclipse.emf.eson.eFactory.EFactoryPackage;
import org.eclipse.emf.eson.eFactory.EnumAttribute;
import org.eclipse.emf.eson.eFactory.Factory;
import org.eclipse.emf.eson.eFactory.Feature;
import org.eclipse.emf.eson.eFactory.IntegerAttribute;
import org.eclipse.emf.eson.eFactory.MultiValue;
import org.eclipse.emf.eson.eFactory.NewObject;
import org.eclipse.emf.eson.eFactory.Reference;
import org.eclipse.emf.eson.eFactory.StringAttribute;
import org.eclipse.emf.eson.eFactory.Value;
import org.eclipse.emf.eson.eFactory.util.EFactorySwitch;
import org.eclipse.emf.eson.resource.EFactoryResource;
import org.eclipse.emf.eson.util.EcoreUtil3;
import org.eclipse.emf.eson.util.XtextProxyUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;

import com.google.common.base.Optional;

/**
 * Validator.
 *  
 * @author Sebastian Benz - initial API and implementation
 * @author Michael Vorburger - extensions and bug fixes
 */
public class EFactoryJavaValidator extends AbstractEFactoryValidator {

	// TODO There is A LOT of unnecessary String concatenation happening here, due to assertTrue.. Measure perf. impact of rewrite as if; suggest to @Deprecated assertTrue? 
	
	public static final String ERR_CANNOT_NAME = "cannotname";
	public static final String ERR_BAD_TYPE = "badtype";

	// NOTE: There are a lot of possible NullPointerException in here in the
	// scenario where some reference types are still proxies.. but the NPEs get
	// swallowed silently by the validation infrastructure
	
	protected @Inject NameAccessor nameAccessor;
	protected @Inject XtextProxyUtil xtextProxyUtil;
	protected @Inject NewObjectExtensions newObjectExtensions;

	protected class AttributeValidator extends EFactorySwitch<Boolean> {

		@Override
		public Boolean caseBooleanAttribute(BooleanAttribute object) {
			EAttribute featureId = EFactoryPackage.Literals.BOOLEAN_ATTRIBUTE__VALUE;
			return performAssert(object, featureId, EcorePackage.Literals.EBOOLEAN, EcorePackage.Literals.EBOOLEAN_OBJECT);
		}

		@Override
		public Boolean caseStringAttribute(StringAttribute object) {
			EAttribute featureId = EFactoryPackage.Literals.STRING_ATTRIBUTE__VALUE;
			return performAssert(object, featureId, EcorePackage.Literals.ESTRING);
		}

		@Override
		public Boolean caseIntegerAttribute(IntegerAttribute object) {
			EAttribute featureId = EFactoryPackage.Literals.INTEGER_ATTRIBUTE__VALUE;
			return performAssert(object, featureId, EcorePackage.Literals.EINT,
					EcorePackage.Literals.EINTEGER_OBJECT,
					EcorePackage.Literals.ELONG,
					EcorePackage.Literals.ELONG_OBJECT,
					EcorePackage.Literals.EBIG_INTEGER,
					EcorePackage.Literals.ESHORT,
					EcorePackage.Literals.ESHORT_OBJECT,
					EcorePackage.Literals.EBYTE,
					EcorePackage.Literals.EBYTE_OBJECT
					);
		}

		@Override
		public Boolean caseEnumAttribute(EnumAttribute object) {
			EReference featureId = EFactoryPackage.Literals.ENUM_ATTRIBUTE__VALUE;
			Feature feature = getFeature(object);
			boolean success = EcoreUtil3.isEnum(feature.getEFeature().getEType());
			if (!success)
				error("Attribute must be of type "
					+ feature.getEFeature().getEType().getName()
					+ " but was an Enumeration", featureId);
			return success;
		}

		@Override
		public Boolean caseMultiValue(MultiValue multiValue) {
			boolean multiValueValidation = true; 
			for (Value value : multiValue.getValues()) {
				if (value instanceof Attribute) {
					if (!this.doSwitch(value))
						multiValueValidation = false;
				}
			}
			return multiValueValidation;
		}

		@Override
		public Boolean caseDoubleAttribute(DoubleAttribute object) {
			EAttribute featureId = EFactoryPackage.Literals.DOUBLE_ATTRIBUTE__VALUE;
			return performAssert(object, featureId, EcorePackage.Literals.EDOUBLE,
					EcorePackage.Literals.EDOUBLE_OBJECT,
					EcorePackage.Literals.EFLOAT,
					EcorePackage.Literals.EFLOAT_OBJECT,
					EcorePackage.Literals.EBIG_DECIMAL);
		}

		private boolean performAssert(Value value, EStructuralFeature featureId,
				EDataType... validDatatypes) {
			Feature feature = getFeature(value);
			EClassifier expected = feature.getEFeature().getEType();

			boolean success = false;
			for (EDataType validDataType : validDatatypes) {
				if (equals(expected, validDataType)) {
					success = true;
					break;
				}
			}
			if (!success) {
				final StringBuffer msg = new StringBuffer("EF Attribute must be one of types ");
				for (EDataType eDataType : validDatatypes) {
					msg.append(eDataType.getName());
					msg.append(", ");
				}
				msg.append("but was ");
				if (feature.getEFeature() != null && feature.getEFeature().getEType() != null)
					msg.append(feature.getEFeature().getEType().getName());
				else
					msg.append("???");
				error(msg.toString(), featureId, ERR_BAD_TYPE);
			}
			return success;
		}

		/* This is required due to strange Data Type validation
		 * mismatch problem (occurs only with Xcore models),
		 * where in the EDataType is not a EcorePackage.Literals.EINT
		 * but has a default of '0' and appears to be from another
		 * ecore EPackage which doesn't have the same object identity.
		 * This is non-regression tested by XcoreTest.
		 */
		private boolean equals(EClassifier expected, EDataType validDataType) {
			if (expected == null)
				return validDataType == null;
			else if (validDataType == null)
				return expected == null;
			else
				return equals(expected.getEPackage(), validDataType.getEPackage())
						&& expected.getName().equals(validDataType.getName());
		}
		
		private boolean equals(EPackage package1, EPackage package2) {
			if (package1 == null)
				return package2 == null; 
			else if (package2 == null)
				return package1 == null; 
			else
				return equals(package1.getESuperPackage(), package2.getESuperPackage())
					&& package1.getName().equals(package2.getName())
					&& package1.getNsPrefix().equals(package2.getNsPrefix())
					&& package1.getNsURI().equals(package2.getNsURI());
		}
	}

	// TODO Better make this Guice injected configurable..
	private final AttributeValidator attributeValidator = new AttributeValidator();

	@Check
	public void checkNewObject(NewObject newObject) {
		checkNoDuplicateFeature(newObject);
		checkIsInstantiatable(newObject);
		checkObjectName(newObject);
		checkEClassNotInSameProject(newObject);
	}

	@Check(CheckType.NORMAL)
	public void checkFactory(Factory factory) {
		final EFactoryResource eFResource = (EFactoryResource) factory.eResource();
		EObject eObject = EFactoryResource.getEFactoryEObject(factory.eResource()) ;
		if (eObject == null) {
			return;
		}
		
		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(eObject);
		for (Diagnostic childDiagnostic : diagnostic.getChildren()) {
			if (childDiagnostic.getSeverity() == Diagnostic.ERROR) {
				// "The feature .. of .. contains an unresolved proxy ..#.."
				// is skipped, for more about why see checkIsBrokenReference(),
				// EFactoryJavaValidatorTest.testOnlyOneErrorForBrokenReference()
				if (childDiagnostic.getCode() != EObjectValidator.EOBJECT__EVERY_PROXY_RESOLVES) {
					error(childDiagnostic.getMessage(),
							getSource(eFResource, diagnostic), null, null);
				}
			} else if (childDiagnostic.getSeverity() == Diagnostic.WARNING) {
				warning(childDiagnostic.getMessage(),
						getSource(eFResource, diagnostic), null, null);
			}
		}
	}

	private EObject getSource(EFactoryResource resource, Diagnostic diagnostic) {
		for (Object data : diagnostic.getData()) {
			if (data instanceof EObject) {
				return resource.getEFactoryNewObject((EObject) data);
			}
		}
		return null;
	}

	private void checkObjectName(NewObject newObject) {
		String name = newObject.getName();
		if (name == null)
			return;
		if (name.trim().isEmpty()) {
			// https://github.com/vorburger/efactory/pull/18
			error("Name cannot be blank", EFactoryPackage.Literals.NEW_OBJECT__NAME);			
		}
		EAttribute nameAttribute = nameAccessor.getNameAttribute(newObject);
		
		Optional<EClass> eClassOptional = newObjectExtensions.getDeclaredOrInferredEClass(newObject);
		if (eClassOptional.isPresent() && nameAttribute == null)
			error("Cannot name " + eClassOptional.get().getName(), EFactoryPackage.Literals.NEW_OBJECT__NAME, ERR_CANNOT_NAME);
	}

	private void checkIsInstantiatable(NewObject newObject) {
		Optional<EClass> eClassOptional = newObjectExtensions.getDeclaredOrInferredEClass(newObject);
		if (!eClassOptional.isPresent())
			return;
		EClass eClass = eClassOptional.get();
		boolean isInstantiatable = EcoreUtil3.isInstantiatable(eClass);
		if (!isInstantiatable)
		    error("Abstract classes or interfaces cannot be instantiated",
				EFactoryPackage.Literals.NEW_OBJECT__ECLASS);
	}

	private void checkNoDuplicateFeature(NewObject newObject) {
		Set<EStructuralFeature> existingFeatures = new HashSet<EStructuralFeature>();
		for (int i = 0; i < newObject.getFeatures().size(); i++) {
			Feature feature = newObject.getFeatures().get(i);
			EStructuralFeature eFeature = feature.getEFeature();
			boolean hasDuplicate = existingFeatures.contains(eFeature);
			if (hasDuplicate)
			    error("Duplicate feature '" + eFeature.getName() + "'",  EFactoryPackage.Literals.NEW_OBJECT__FEATURES, i);
			existingFeatures.add(eFeature);
		}
	}
	
	@Check
	public void checkFeature(Feature feature) {
		EStructuralFeature eFeature = feature.getEFeature();
		if (eFeature == null || eFeature.eIsProxy()) {
			return;
		}
		checkCardinality(feature);
		checkIsFeature(feature);
		checkMissingFeatureValue(feature);
	}

	private void checkMissingFeatureValue(Feature feature) {
		EStructuralFeature eFeature = feature.getEFeature();
		if (feature.getValue() == null)
			error("Feature missing value: " + eFeature.getName(), EFactoryPackage.Literals.FEATURE__VALUE);
	}

	private void checkIsFeature(Feature feature) {
		NewObject newObject = getNewObject(feature);
		Optional<EClass> eClassOptional = newObjectExtensions.getDeclaredOrInferredEClass(newObject);
		if (!eClassOptional.isPresent())
			return;
		EClass eClass = eClassOptional.get();

		if (!hasEFeature(eClass, feature.getEFeature())) {
			error(eClass.getName() + " has no feature "
					+ feature.getEFeature().getName(),
					EFactoryPackage.Literals.FEATURE__EFEATURE);
		}
	}

	private boolean hasEFeature(EClass eClass, EStructuralFeature feature) {
		return eClass.getEAllStructuralFeatures().contains(feature);
	}

	private void checkCardinality(Feature feature) {
		final boolean hasMany = feature.getEFeature().isMany();
		final Value value = feature.getValue();
		if (value == null)
			return;
		if (value instanceof MultiValue) {
			if (!hasMany)
				error("Cannot assign multiple elements to a feature with cardinality 1 (remove [...])",
					EFactoryPackage.Literals.FEATURE__VALUE);
		} else {
			if (hasMany)
				error("Cannot assign a single element to a feature with cardinality >1 (use [...])",
					EFactoryPackage.Literals.FEATURE__VALUE);
		}
	}

	@Check
	public void checkReference(Reference reference) {
		Feature feature = getFeature(reference);
		EStructuralFeature eFeature = feature.getEFeature();
		if (eFeature == null || eFeature.eIsProxy()) {
			return;
		}
		checkIsEReference(eFeature);
		checkIsNotContainment(eFeature);
		if (!checkIsBrokenReference(eFeature, reference)) {
			// Only check assign-ability if it's not a broken reference
			// (otherwise the extra error is only confusing, and doesn't help)
			checkIsAssignable(eFeature,
					EFactoryPackage.Literals.REFERENCE__VALUE,
					getReferencedType(reference));
		}
	}

	private EClass getReferencedType(Reference reference) {
		EObject referenceContainer = reference.eContainer();
		while (referenceContainer instanceof MultiValue) {
			referenceContainer = referenceContainer.eContainer();
		}
		if (!(referenceContainer instanceof Feature)) {
			// Technically this should never happen,
			// according to current EFactory.xtext grammar
			return EcorePackage.Literals.EOBJECT;
		}
		Feature referenceFeature = (Feature) referenceContainer;
		EStructuralFeature referenceEFeature = referenceFeature.getEFeature();
		if (!(referenceEFeature instanceof EReference)) {
			// Technically this should never happen..
			return EcorePackage.Literals.EOBJECT;
		}
		EReference referenceEReference = (EReference) referenceEFeature;
		return referenceEReference.getEReferenceType();
	}

	private boolean checkIsBrokenReference(EStructuralFeature eFeature, Reference reference) {
		// See comment in getReferencedType() above.. technically this could be
		// optimized by just looking into the index to see if ref. name is
		// known, instead of actually calling getValue() causing proxy
		// resolution.
		EObject refObj = reference.getValue();
		if (!refObj.eIsProxy()) {
			return false;
		} else {
			String referenceTypeName = getReferencedType(reference).getName();
			String crossRefText = xtextProxyUtil.getProxyCrossRefAsString(reference, refObj);
			error("Unknown " + referenceTypeName + ": " + crossRefText,
					reference, EFactoryPackage.Literals.REFERENCE__VALUE,
					// It's super important that we use this std. Xtext error
					// code, so that the Quick Fix for broken references still
					// works as it would had the standard 
					// LinkingDiagnosticMessageProvider
					// instead of us here added this error
					org.eclipse.xtext.diagnostics.Diagnostic.LINKING_DIAGNOSTIC);
			return true;
		}
	}

	private void checkIsAssignable(EStructuralFeature eFeature,
			EReference containmentValue, EClass candidate) {
		EClassifier eType = eFeature.getEType();
		if (eType == null)
			return;
		if (eType instanceof EClass) {
			EClass eClass = (EClass) eType;
			if (!EcoreUtil2.isAssignableFrom(eClass, candidate)) {
				error("Wrong type. Expected instance of '" + eType.getName()
						+ "'", containmentValue);
			}
		}
	}

	private void checkIsNotContainment(EStructuralFeature eStructuralFeature) {
		boolean isContainment = isContainment(eStructuralFeature);
		if (isContainment)
			error("Value must be a reference but is a containment",
				EFactoryPackage.Literals.REFERENCE__VALUE);
	}

	private boolean isContainment(EStructuralFeature eStructuralFeature) {
		return EcoreUtil3.isEContainment(eStructuralFeature);
	}

	private void checkIsEReference(EStructuralFeature eFeature) {
		if (!isEReference(eFeature))
			error("Value must be a reference but is an attribute",
				EFactoryPackage.Literals.REFERENCE__VALUE);
	}

	private boolean isEReference(EStructuralFeature eStructuralFeature) {
		return EcoreUtil3.isEReference(eStructuralFeature);
	}

	@Check
	public void checkContainment(Containment containment) {
		Feature feature = getFeature(containment);
		EStructuralFeature eFeature = feature.getEFeature();
		if (eFeature == null || eFeature.eIsProxy()) {
			return;
		}
		checkIsContainment(eFeature);
		EClass declaredEClass = containment.getValue().getEClass();
		if (declaredEClass != null)
			checkIsAssignable(eFeature, EFactoryPackage.Literals.CONTAINMENT__VALUE, declaredEClass);
	}

	private void checkIsContainment(EStructuralFeature eFeature) {
		if (!isContainment(eFeature))
		    error("Value must be a new object but is a reference",
				EFactoryPackage.Literals.FEATURE__VALUE);
	}

	@Check
	public void checkAttribute(Attribute attribute) {
		Feature feature = getFeature(attribute);
		if (isEReference(feature.getEFeature()))
		    error("Value must be an attribute but is a reference", null);

		attributeValidator.doSwitch(attribute);
	}

	private Feature getFeature(Value value) {
		Feature feature = EcoreUtil2.getContainerOfType(value, Feature.class);
		if (feature == null)
			throw new NoSuchElementException("EFactory Value " + value.toString() + " is not contained in a Feature?!");
		return feature;
	}

	private NewObject getNewObject(Feature feature) {
		NewObject newObject = EcoreUtil2.getContainerOfType(feature, NewObject.class);
		if (newObject == null)
			throw new NoSuchElementException("EFactory Feature " + feature.getEFeature().getName() + " is not contained in a NewObject?!");
		return newObject;
	}

	// @see Documentation in EFactoryDerivedStateComputer
	private void checkEClassNotInSameProject(NewObject newObject) {
		URI newObjectURI = newObject.eResource().getURI();
		Optional<EClass> eClassOptional = newObjectExtensions.getDeclaredOrInferredEClass(newObject);
		if (!eClassOptional.isPresent())
			return;
		EClass eClass = eClassOptional.get();
		URI eClassURI = eClass.eResource().getURI();
		if (isInSameProject(eClassURI, newObjectURI)) {
			error("EClass must be in different project than ESON (so that it gets separately indexed first): " + eClass.getName(), EFactoryPackage.Literals.NEW_OBJECT__ECLASS);
		}
	}

	public static boolean isInSameProject(URI uri1, URI uri2) {
		if (!uri1.isPlatformResource() || !uri2.isPlatformResource())
			return false;
		return uri1.segment(1).equals(uri2.segment(1));
	}
	
}
