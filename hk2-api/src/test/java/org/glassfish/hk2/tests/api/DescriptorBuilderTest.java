/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.hk2.tests.api;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.DescriptorImpl;
import org.junit.Test;

/**
 * @author jwells
 *
 */
public class DescriptorBuilderTest {
    private final static String FACTORY_CLASS_NAME = "my.factory.Factory";
    private final static String NAME = "name";
    private final static String CONTRACT_NAME = "my.factory.Contract";
    private final static int MY_RANK = 13;
    private final static String KEY = "key";
    private final static String VALUE = "value";
    
    /**
     * Tests factory production
     */
    @Test
    public void testBuildFactoryNoArg() {
        BlueImpl blue = new BlueImpl();
        HK2LoaderImpl loader = new HK2LoaderImpl();
        
        FactoryDescriptors fds = BuilderHelper.link(FACTORY_CLASS_NAME).
            named(NAME).
            to(CONTRACT_NAME).
            in(Singleton.class).
            qualifiedBy(blue).
            ofRank(MY_RANK).
            andLoadWith(loader).
            has(KEY, VALUE).
            proxy(false).
            localOnly().
            buildFactory();
        
        {
            // Now ensure the resulting descriptors have the expected results
            Descriptor asService = fds.getFactoryAsAService();
        
            // The javadoc says the return from buildFactory will have DescriptorImpl
            Assert.assertTrue(asService instanceof DescriptorImpl);
        
            Assert.assertEquals(DescriptorType.CLASS, asService.getDescriptorType());
            Assert.assertEquals(DescriptorVisibility.NORMAL, asService.getDescriptorVisibility());
            Assert.assertEquals(FACTORY_CLASS_NAME, asService.getImplementation());
            Assert.assertEquals(PerLookup.class.getName(), asService.getScope());
            Assert.assertEquals(MY_RANK, asService.getRanking());
            Assert.assertNull(asService.getName());
            Assert.assertTrue(asService.getQualifiers().isEmpty());
            Assert.assertTrue(asService.getMetadata().isEmpty());
            Assert.assertEquals(loader, asService.getLoader());
            Assert.assertNull(asService.isProxiable());
        
            Set<String> serviceContracts = asService.getAdvertisedContracts();
            Assert.assertEquals(2, serviceContracts.size());
            Assert.assertTrue(serviceContracts.contains(FACTORY_CLASS_NAME));
            Assert.assertTrue(serviceContracts.contains(Factory.class.getName()));
        }
        
        {
            // Now ensure the resulting descriptors have the expected results
            Descriptor asFactory = fds.getFactoryAsAFactory();
        
            // The javadoc says the return from buildFactory will have DescriptorImpl
            Assert.assertTrue(asFactory instanceof DescriptorImpl);
        
            Assert.assertEquals(DescriptorType.PROVIDE_METHOD, asFactory.getDescriptorType());
            Assert.assertEquals(DescriptorVisibility.LOCAL, asFactory.getDescriptorVisibility());
            Assert.assertEquals(FACTORY_CLASS_NAME, asFactory.getImplementation());
            Assert.assertEquals(Singleton.class.getName(), asFactory.getScope());
            Assert.assertEquals(MY_RANK, asFactory.getRanking());
            Assert.assertEquals(NAME, asFactory.getName());
            Assert.assertEquals(loader, asFactory.getLoader());
            Assert.assertEquals(false, asFactory.isProxiable().booleanValue());
            
            Set<String> qualifiers = asFactory.getQualifiers();
            Assert.assertEquals(2, qualifiers.size());
            Assert.assertTrue(qualifiers.contains(Blue.class.getName()));
            Assert.assertTrue(qualifiers.contains(Named.class.getName()));
            
            Map<String, List<String>> metadata = asFactory.getMetadata();
            Assert.assertEquals(1, metadata.size());
            Assert.assertTrue(metadata.containsKey(KEY));
            
            List<String> values = metadata.get(KEY);
            Assert.assertEquals(1, values.size());
            Assert.assertEquals(VALUE, values.get(0));
        
            Set<String> serviceContracts = asFactory.getAdvertisedContracts();
            Assert.assertEquals(1, serviceContracts.size());
            Assert.assertTrue(serviceContracts.contains(CONTRACT_NAME));
        }
        
        Assert.assertTrue(fds.toString().contains("descriptorType=PROVIDE_METHOD"));
        Assert.assertTrue(fds.toString().contains("descriptorType=CLASS"));
    }
    
    /**
     * Tests factory production
     */
    @Test
    public void testImplNotAddedToContract() {
        DescriptorImpl desc = BuilderHelper.link(FACTORY_CLASS_NAME, false).
            to(CONTRACT_NAME).
            build();
        
        Assert.assertEquals(DescriptorType.CLASS, desc.getDescriptorType());
        Assert.assertEquals(FACTORY_CLASS_NAME, desc.getImplementation());
        Assert.assertNull(desc.getScope());
        Assert.assertEquals(0, desc.getRanking());
        Assert.assertNull(desc.getName());
        Assert.assertTrue(desc.getQualifiers().isEmpty());
        Assert.assertTrue(desc.getMetadata().isEmpty());
        Assert.assertNull(desc.getLoader());
        
        Set<String> serviceContracts = desc.getAdvertisedContracts();
        Assert.assertEquals(1, serviceContracts.size());
        Assert.assertTrue(serviceContracts.contains(CONTRACT_NAME));
    }
    
    /**
     * Tests double name
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNotNamedTwice() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          named(NAME).
          named(NAME);
    }
    
    /**
     * Tests double loader
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNotLoadedTwice() {
        HK2Loader loader = new HK2LoaderImpl();
        
        BuilderHelper.link(FACTORY_CLASS_NAME).
          andLoadWith(loader).
          andLoadWith(loader);
    }
    
    /**
     * Tests illegal null contract
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalClassContract() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          to((Class<?>) null);
    }
    
    /**
     * Tests illegal null contract
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalStringContract() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          to((String) null);
    }
    
    /**
     * Tests illegal null scope
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalClassScope() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          in((Class<? extends Annotation>) null);
    }
    
    /**
     * Tests illegal null scope
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalStringScope() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          in((String) null);
    }
    
    /**
     * Tests illegal null qualifier
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalClassQualifier() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          qualifiedBy((Annotation) null);
    }
    
    /**
     * Tests illegal null qualifier
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalStringQualifier() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          qualifiedBy((String) null);
    }
    
    /**
     * Tests illegal null key
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalMetadataKey() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          has(null, VALUE);
    }
    
    /**
     * Tests illegal null value
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalMetadataValue() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          has(KEY, (String) null);
    }
    
    /**
     * Tests illegal null list key
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalMetadataListKey() {
        LinkedList<String> values = new LinkedList<String>();
        values.add(VALUE);
        
        BuilderHelper.link(FACTORY_CLASS_NAME).
          has(null, values);
    }
    
    /**
     * Tests illegal null value
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalMetadataListNullValue() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          has(KEY, (List<String>) null);
    }
    
    /**
     * Tests illegal empty value list
     */
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalMetadataListEmptyValue() {
        BuilderHelper.link(FACTORY_CLASS_NAME).
          has(KEY, new LinkedList<String>());
    }

}
