package org.apache.commons.configuration;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

/**
 * Test loading multiple configurations.
 *
 * @version $Id: TestCompositeConfiguration.java,v 1.7 2004/04/01 18:43:03 epugh Exp $
 */
public class TestCompositeConfiguration extends TestCase
{
    protected BasePropertiesConfiguration conf1;
    protected BasePropertiesConfiguration conf2;
    protected DOM4JConfiguration dom4jConf;
    protected CompositeConfiguration cc;
    /** The File that we test with */
    private String testProperties = new File("conf/test.properties").getAbsolutePath();
    private String testProperties2 = new File("conf/test2.properties").getAbsolutePath();
    private String testPropertiesXML = new File("conf/test.xml").getAbsolutePath();

    protected void setUp() throws Exception
    {
        cc = new CompositeConfiguration();
        conf1 = new PropertiesConfiguration(testProperties);
        conf2 = new PropertiesConfiguration(testProperties2);
        dom4jConf = new DOM4JConfiguration(new File(testPropertiesXML));
    }

    public void testAddRemoveConfigurations() throws Exception
    {

        cc.addConfiguration(conf1);
        assertEquals(2, cc.getNumberOfConfigurations());
        cc.addConfiguration(conf1);
        assertEquals(2, cc.getNumberOfConfigurations());
        cc.addConfiguration(conf2);
        assertEquals(3, cc.getNumberOfConfigurations());
        cc.removeConfiguration(conf1);
        assertEquals(2, cc.getNumberOfConfigurations());
        cc.clear();
        assertEquals(1, cc.getNumberOfConfigurations());
    }

    public void testGetPropertyWIncludes() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(conf2);
        List l = cc.getList("packages");
        assertTrue(l.contains("packagea"));
    }
    
    public void testGetProperty() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(conf2);
        assertEquals("Make sure we get the property from conf1 first", "test.properties", cc.getString("propertyInOrder"));
        cc.clear();

        cc.addConfiguration(conf2);
        cc.addConfiguration(conf1);
        assertEquals("Make sure we get the property from conf2 first", "test2.properties", cc.getString("propertyInOrder"));
    }

    public void testCantRemoveMemoryConfig() throws Exception
    {
        cc.clear();
        assertEquals(1, cc.getNumberOfConfigurations());

        Configuration internal = cc.getConfiguration(0);
        cc.removeConfiguration(internal);

        assertEquals(1, cc.getNumberOfConfigurations());

    }

    public void testGetPropertyMissing() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(conf2);
        try{
            assertNull(cc.getString("bogus.property"));
            fail("Should have thrown a NoSuchElementException");
        }
        catch(NoSuchElementException nsee){
            assertTrue(nsee.getMessage().indexOf("bogus.property")>-1);
        }

        assertTrue("Should be false", !cc.getBoolean("test.missing.boolean", false));
        assertTrue("Should be true", cc.getBoolean("test.missing.boolean.true", true));

    }

    /**
     * Tests <code>List</code> parsing.
     */
    public void testMultipleTypesOfConfigs() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        assertEquals("Make sure we get the property from conf1 first", 1, cc.getInt("test.short"));
        cc.clear();

        cc.addConfiguration(dom4jConf);
        cc.addConfiguration(conf1);
        assertEquals("Make sure we get the property from dom4j", 8, cc.getInt("test.short"));
    }

    /**
     * Tests <code>List</code> parsing.
     */
    public void testPropertyExistsInOnlyOneConfig() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        assertEquals("value", cc.getString("element"));
    }

    /**
     * Tests getting a default when the key doesn't exist
     */
    public void testDefaultValueWhenKeyMissing() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        assertEquals("default", cc.getString("bogus", "default"));
        assertTrue(1.4 == cc.getDouble("bogus", 1.4));
        assertTrue(1.4 == cc.getDouble("bogus", 1.4));
    }

    /**
     * Tests <code>List</code> parsing.
     */
    public void testGettingConfiguration() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        assertEquals(PropertiesConfiguration.class, cc.getConfiguration(0).getClass());
        assertEquals(DOM4JConfiguration.class, cc.getConfiguration(1).getClass());
    }

    /**
     * Tests setting values.  These are set in memory mode only!
     */
    public void testClearingProperty() throws Exception
    {

        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        cc.clearProperty("test.short");
        assertTrue("Make sure test.short is gone!", !cc.containsKey("test.short"));
    }

    /**
     * Tests adding values.  Make sure they _DON'T_ override any other properties but add to the
     * existing properties  and keep sequence
     */
    public void testAddingProperty() throws Exception
    {

        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);

        String [] values = cc.getStringArray("test.short");

        assertEquals("Number of values before add is wrong!", 2, values.length);
        assertEquals("First Value before add is wrong", "1", values[0]);
        assertEquals("Second Value is wrong", "8", values[1]);

        cc.addProperty("test.short", "88");

        values = cc.getStringArray("test.short");

        assertEquals("Number of values is wrong!", 3, values.length);
        assertEquals("First Value is wrong", "1", values[0]);
        assertEquals("Second Value is wrong", "8", values[1]);
        assertEquals("Third Value is wrong", "88", values[2]);
    }

    /**
     * Tests setting values.  These are set in memory mode only!
     */
    public void testSettingMissingProperty() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);
        cc.setProperty("my.new.property", "supernew");
        assertEquals("supernew", cc.getString("my.new.property"));
    }

    /**
     * Tests retrieving subsets of configurations
     */
    public void testGettingSubset() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);

        Configuration subset = null;
        subset = cc.subset("test");
        assertNotNull(subset);
        assertFalse("Shouldn't be empty", subset.isEmpty());
        assertEquals("Make sure the initial loaded configs subset overrides any later add configs subset", "1", subset.getString("short"));

        cc.setProperty("test.short", "43");
        subset = cc.subset("test");
        assertEquals("Make sure the initial loaded configs subset overrides any later add configs subset", "43", subset.getString("short"));
    }

    /**
     * Tests subsets and still can resolve elements
     */
    public void testSubsetCanResolve() throws Exception
    {
        cc = new CompositeConfiguration();
        final BaseConfiguration config = new BaseConfiguration();
        config.addProperty("subset.tempfile", "${java.io.tmpdir}/file.tmp");
        cc.addConfiguration(config);
        cc.addConfiguration(ConfigurationConverter.getConfiguration(System.getProperties()));

        Configuration subset = cc.subset("subset");
        assertEquals(System.getProperty("java.io.tmpdir") + "/file.tmp", subset.getString("tempfile"));
    }

    /**
      * Tests <code>List</code> parsing.
      */
    public void testList() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);

        List packages = cc.getList("packages");
        // we should get 3 packages here
        assertEquals(3, packages.size());

        List defaultList = new ArrayList();
        defaultList.add("1");
        defaultList.add("2");

        packages = cc.getList("packages.which.dont.exist", defaultList);
        // we should get 2 packages here
        assertEquals(2, packages.size());
    }

    /**
      * Tests <code>String</code> array parsing.
      */
    public void testStringArray() throws Exception
    {
        cc.addConfiguration(conf1);
        cc.addConfiguration(dom4jConf);

        String[] packages = cc.getStringArray("packages");
        // we should get 3 packages here
        assertEquals(3, packages.length);

        packages = cc.getStringArray("packages.which.dont.exist");
        // we should get 0 packages here
        assertEquals(0, packages.length);
    }
    
    /**
      * Tests <code>getKeys</code> preserves the order
      */
    public void testGetKeysPreservesOrder() throws Exception
    {
        cc.addConfiguration(conf1);
        List orderedList = new ArrayList();
        for (Iterator keys = conf1.getKeys();keys.hasNext();){
            orderedList.add(keys.next());
        }
        List iteratedList = new ArrayList();
        for (Iterator keys = cc.getKeys();keys.hasNext();){
            iteratedList.add(keys.next());
        }
        assertEquals(orderedList.size(),iteratedList.size());
        for (int i =0;i<orderedList.size();i++){
            assertEquals(orderedList.get(i),iteratedList.get(i));
        }        
    }    

    /**
      * Tests <code>getKeys(String key)</code> preserves the order
      */
    public void testGetKeys2PreservesOrder() throws Exception
    {
        cc.addConfiguration(conf1);
        List orderedList = new ArrayList();
        for (Iterator keys = conf1.getKeys("test");keys.hasNext();){
            orderedList.add(keys.next());
        }
        List iteratedList = new ArrayList();
        for (Iterator keys = cc.getKeys("test");keys.hasNext();){
            iteratedList.add(keys.next());
        }
        assertEquals(orderedList.size(),iteratedList.size());
        for (int i =0;i<orderedList.size();i++){
            assertEquals(orderedList.get(i),iteratedList.get(i));
        }        
    }        
    
    public void testGetStringWithDefaults()
    {
        BaseConfiguration defaults = new BaseConfiguration();
        defaults.addProperty("default", "default string");

        Configuration c = new CompositeConfiguration(defaults);
        
        c.addProperty("string", "test string");

        assertEquals("test string", c.getString("string"));
        try
        {
            c.getString("XXX");
            fail("Should throw NoSuchElementException exception");
        }
        catch (NoSuchElementException e)
        {
            //ok
        }
        catch (Exception e)
        {
            fail("Should throw NoSuchElementException exception, not " + e);
        }

        //test defaults
        assertEquals(
            "test string",
            c.getString("string", "some default value"));
        assertEquals("default string", c.getString("default"));
        assertEquals(
            "default string",
            c.getString("default", "some default value"));
        assertEquals(
            "some default value",
            c.getString("XXX", "some default value"));
    }
    
    public void testCheckingInMemoryConfiguration() throws Exception{
        String TEST_KEY = "testKey";
        Configuration defaults = new PropertiesConfiguration();
        defaults.setProperty(TEST_KEY,"testValue");
        Configuration testConfiguration = new CompositeConfiguration(defaults);
        assertTrue(testConfiguration.containsKey(TEST_KEY));
        assertFalse(testConfiguration.isEmpty());
        boolean foundTestKey = false;
        Iterator i = testConfiguration.getKeys();
        //assertTrue(i instanceof IteratorChain);
        //IteratorChain ic = (IteratorChain)i;
        //assertEquals(2,i.size());
        for (;i.hasNext();){
            String key = (String)i.next();
            if(key.equals(TEST_KEY)){
                foundTestKey = true;
            }
        }
        assertTrue(foundTestKey);
        testConfiguration.clearProperty(TEST_KEY);
        assertFalse(testConfiguration.containsKey(TEST_KEY));
        
    }    
}
