package ca.xshade.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Used for accessing and creating .[properties] files, reads them as utf-8, saves as utf-8.
 * Internationalization is key importance especially for character codes.
 *
 * @author Nijikokun
 * @version 1.0.4, %G%
 */
public final class PropertiesFile {

    private static final Logger log = Logger.getLogger("Minecraft");
    private String fileName;
    private Properties props = new Properties();
//    private List<String> lines = new ArrayList<String>();
//    private Map<String, String> props = new HashMap<String, String>();

    /**
     * Creates or opens a properties file using specified filename
     *
     * @param fileName
     */
    public PropertiesFile(String fileName) {
        this.fileName = fileName;

        File file = new File(fileName);

        try {
            if (file.exists())
				load();
			else
				save();
        } catch (IOException ex) {
            log.severe("[PropertiesFile] Unable to load " + fileName + "!");
        }
    }

    /**
     * The loader for property files, it reads the file as UTF8 or converts the string into UTF8.
     * Used for simple runthrough's, loading, or reloading of the file.
     *
     * @throws IOException
     */
    public void load() throws IOException {
        props.load(new FileInputStream(fileName));
    }

    /**
     * Writes out the <code>key=value</code> properties that were changed into
     * a .[properties] file in UTF8.
     */
    public void save() {
        try {
        props.store(new FileOutputStream(fileName), null);
        }catch(IOException ex) {
        }
    }

    /**
     * Returns a Map of all <code>key=value</code> properties in the file as <code>&lt;key (java.lang.String), value (java.lang.String)></code>
     * <br /><br />
     * Example:
     * <blockquote><pre>
     * PropertiesFile settings = new PropertiesFile("settings.properties");
     * Map<String, String> mappedSettings;
     * 
     * try {
     * 	 mappedSettings = settings.returnMap();
     * } catch (Exception ex) {
     * 	 log.info("Failed mapping settings.properties");
     * }
     * </pre></blockquote>
     *
     * @return <code>map</code> - Simple Map HashMap of the entire <code>key=value</code> as <code>&lt;key (java.lang.String), value (java.lang.String)></code>
     * @throws Exception If the properties file doesn't exist.
     */
    
	public Map<String, String> returnMap() throws Exception {
        return (Map<String, String>) props.clone();
    }

    /**
     * Checks to see if the .[properties] file contains the given <code>key</code>.
     *
     * @param var The key we are going to be checking the existance of.
     * @return <code>Boolean</code> - True if the <code>key</code> exists, false if it cannot be found.
     */
    public boolean containsKey(String var) {
        return props.containsKey(var);
    }

    /**
     * Checks to see if this <code>key</code> exists in the .[properties] file.
     *
     * @param var The key we are grabbing the value of.
     * @return <code>java.lang.String</code> - True if the <code>key</code> exists, false if it cannot be found.
     */
    public String getProperty(String var) {
        return (String)props.getProperty(var);
    }

    /**
     * Remove a key from the file if it exists.
     * This will save() which will invoke a load() on the file.
     *
     * @see #save()
     * @param var The <code>key</code> that will be removed from the file
     */
    public void removeKey(String var) {
        if (this.props.containsKey(var)) {
            this.props.remove(var);
            save();
        }
    }

    /**
     * Checks the existance of a <code>key</code>.
     *
     * @see #containsKey(java.lang.String)
     * @param key The <code>key</code> in question of existance.
     * @return <code>Boolean</code> - True for existance, false for <code>key</code> found.
     */
    public boolean keyExists(String key) {
        return containsKey(key);
    }

    /**
     * Returns the value of the <code>key</code> given as a <code>String</code>,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @see #getProperty(java.lang.String)
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to "" or empty.
     */
    public String getString(String key) {
        if (this.containsKey(key))
			return this.getProperty(key);

        return "";
    }

    /**
     * Returns the value of the <code>key</code> given as a <code>String</code>.
     * If it is not found, it will invoke saving the default <code>value</code> to the properties file.
     *
     * @see #setString(java.lang.String, java.lang.String)
     * @see #getProperty(java.lang.String)
     * @param key The key that we will be grabbing the value from, if no value is found set and return <code>value</code>
     * @param value The default value that we will be setting if no prior <code>key</code> is found.
     * @return java.lang.String Either we will return the default value or a prior existing value depending on existance.
     */
    public String getString(String key, String value) {
        if (this.containsKey(key))
			return this.getProperty(key);

        setString(key, value);
        return value;
    }

    /**
     * Save the value given as a <code>String</code> on the specified key.
     *
     * @see #save()
     * @param key The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     */
    public void setString(String key, String value) {
        props.put(key, value);
        save();
    }

    /**
     * Returns the value of the <code>key</code> given in a Integer,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @see #getProperty(String var)
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0
     */
    public int getInt(String key) {
        if (this.containsKey(key))
			return Integer.parseInt(this.getProperty(key));

        return 0;
    }

    /**
     * Returns the int value of a key
     *
     * @see #setInt(String key, int value)
     * @param key The key that we will be grabbing the value from, if no value is found set and return <code>value</code>
     * @param value The default value that we will be setting if no prior <code>key</code> is found.
     * @return <code>Integer</code> - Either we will return the default value or a prior existing value depending on existance.
     */
    public int getInt(String key, int value) {
        if (this.containsKey(key))
			return Integer.parseInt(this.getProperty(key));

        setInt(key, value);
        return value;

    }

    /**
     * Save the value given as a <code>int</code> on the specified key.
     *
     * @see #save()
     * @param key The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     */
    public void setInt(String key, int value) {
        props.put(key, String.valueOf(value));

        save();
    }

    /**
     * Returns the value of the <code>key</code> given in a Double,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @see #getProperty(String var)
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0.0
     */
    public double getDouble(String key) {
        if (this.containsKey(key))
			return Double.parseDouble(this.getProperty(key));

        return 0;
    }

    /**
     * Returns the double value of a key
     *
     * @see #setDouble(String key, double value)
     * @param key The key that we will be grabbing the value from, if no value is found set and return <code>value</code>
     * @param value The default value that we will be setting if no prior <code>key</code> is found.
     * @return <code>Double</code> - Either we will return the default value or a prior existing value depending on existance.
     */
    public double getDouble(String key, double value) {
        if (this.containsKey(key))
			return Double.parseDouble(this.getProperty(key));

        setDouble(key, value);
        return value;
    }

    /**
     * Save the value given as a <code>double</code> on the specified key.
     *
     * @see #save()
     * @param key The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     */
    public void setDouble(String key, double value) {
        props.put(key, String.valueOf(value));

        save();
    }

    /**
     * Returns the value of the <code>key</code> given in a Long,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @see #getProperty(String var)
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0L
     */
    public long getLong(String key) {
        if (this.containsKey(key))
			return Long.parseLong(this.getProperty(key));

        return 0;
    }

    /**
     * Returns the long value of a key
     *
     * @see #setLong(String key, long value)
     * @param key The key that we will be grabbing the value from, if no value is found set and return <code>value</code>
     * @param value The default value that we will be setting if no prior <code>key</code> is found.
     * @return <code>Long</code> - Either we will return the default value or a prior existing value depending on existance.
     */
    public long getLong(String key, long value) {
        if (this.containsKey(key))
			return Long.parseLong(this.getProperty(key));

        setLong(key, value);
        return value;
    }

    /**
     * Save the value given as a <code>long</code> on the specified key.
     *
     * @see #save()
     * @param key The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     */
    public void setLong(String key, long value) {
        props.put(key, String.valueOf(value));

        save();
    }

    /**
     * Returns the value of the <code>key</code> given in a Boolean,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @see #getProperty(String var)
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to false
     */
    public boolean getBoolean(String key) {
        if (this.containsKey(key))
			return Boolean.parseBoolean(this.getProperty(key));

        return false;
    }

    /**
     * Returns the boolean value of a key
     *
     * @see #setBoolean(String key, boolean value)
     * @param key The key that we will be grabbing the value from, if no value is found set and return <code>value</code>
     * @param value The default value that we will be setting if no prior <code>key</code> is found.
     * @return <code>Boolean</code> - Either we will return the default value or a prior existing value depending on existance.
     */
    public boolean getBoolean(String key, boolean value) {
        if (this.containsKey(key))
			return Boolean.parseBoolean(this.getProperty(key));

        setBoolean(key, value);
        return value;
    }

    /**
     * Save the value given as a <code>boolean</code> on the specified key.
     *
     * @see #save()
     * @param key The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     */
    public void setBoolean(String key, boolean value) {
        props.put(key, String.valueOf(value));

        save();
    }
}
