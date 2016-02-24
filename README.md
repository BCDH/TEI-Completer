# TEI-Completer
A plug-in for enabling and setting up customizable server queries for attribute value completion in oXygen XML Editor


# Installation

1. Copy the file `tei-completer-1.0-SNAPSHOT-shaded.jar` to `$OXYGEN_HOME/frameworks/tei`.

The location of `$OXYGEN_HOME` will depend on where you have installed Oxygen XML Editor. The following are the known
default locations for Oxygen:

  * Mac OS X: `/Applications/oxygen`

  * Linux: `~/Oxygen XML Editor 17`, or if *sudo*: `/opt/Oxygen XML Editor 17`

  * Windows: `C:\Program Files\Oxygen XML Editor 17`

2. Create a [Configuration File](#configuring)

3. Start Oxygen XML Editor.

4. In Oxygen, click on the *File* -> *Preferences* menu, or if you are on Mac OS X then the *Oxygen XML Editor* -> *Preferences* menu.

5. Select and expand the *Document Type Associaton* item from the left panel.

6. Scroll down and select the `TEI P5` Document Type Association. ![alt text](https://raw.githubusercontent.com/BCDH/TEI-Completer/master/doc/images/oxygen-document-type-associations-tei-p5.jpg "Oxygen Document Type Associations")

7. Click the *Edit* button under the list of Document Type Associations

8. Select the *Classpath* tab, and then click on the `+` button under the list of paths.

9. In the dialog box *Add resources to the classpath*, enter the following text `${framework}/tei-completer-1.0-SNAPSHOT-shaded.jar`, and then click the *OK* button. ![alt text](https://raw.githubusercontent.com/BCDH/TEI-Completer/master/doc/images/oxygen-edit-tei-p5-document-type-association.jpg "Editing the TEI P5 framework classpath")

10. Select the *Extensions* tab, and then click the *Choose* button beside the entry for *Content completion handler*.

11. Select the `TEI Completer - org.humanistika.oxygen.tei.completer` plugin, and then click the *OK* button. ![alt text](https://raw.githubusercontent.com/BCDH/TEI-Completer/master/doc/images/oxygen-edit-tei-p5-content-completion-handler.jpg "Editing the TEI P5 Content completion handler")

12. Click the *OK* button to leave the TEI P5 Document Type association dialog, click the *OK* button again to leave the Oxygen Preferences dialog.


# Configuring

The TEI Completer uses an XML configuration file whoose syntax is documented in the XML Schema [config.xsd](https://raw.githubusercontent.com/BCDH/TEI-Completer/master/src/main/resources/config.xsd).

The XML file must be named `config.xml` and placed in a folder named `.bcdh-tei-completer` in your user profile. The
following are the known locations for the config file:

  * Mac OS X: `~/.bcdh-tei-completer/config.xml`

  * Linux: `~/.bcdh-tei-completer/config.xml`

  * Windows: `%USER_PROFILE%\Application Data\.bcdh-tei-completer\config.xml`


A simple `config.xml` which simply requests auto-completion suggestions for all `//w/@lemma` attributes from a server
which also considers the value of the dependent attribute `//w/@ana` value, would look like:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://humanistika.org/ns/tei-completer">
    <server>
        <username>my-username</username>
        <password>my-password</password>
        <baseUrl>http://my-server.com/multext</baseUrl>
    </server>
    <autoComplete>
        <context>//w</context>
        <attribute>@lemma</attribute>
        <dependent default="default-ana">@ana</dependent>
        <selection>./text()</selection>
        <request>
            <url>$baseUrl/getlemma/$selection/$dependent</url>
        </request>
        <response>
            <transformation>getLemmaOutput.xslt</transformation>
        </response>
    </autoComplete>
</config>
```



***NOTE*** Changes to the configuration require restarting Oxygen to be detected.


# Server Messages

Servers are expected to respond to the plugin using an XML or JSON document, which contains the suggestions for
auto-completion. The XML format is documented in [suggestions.xsd](https://raw.githubusercontent.com/BCDH/TEI-Completer/master/src/main/resources/suggestions.xsd).
The JSON format is a direct conversion of the XML format.


* Example of the XML format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suggestions xmlns="http://humanistika.org/ns/tei-completer">
    <suggestion>
        <value>suggestion1</value>
        <description>A description of suggestion 1</description>
    </suggestion>
    <suggestion>
        <value>suggestion2</value>
        <description>A description of suggestion 2</description>
    </suggestion>
</suggestions>
```

* Example of the JSON format:

```json
{
    "tc:suggestion": [
        {
            "tc:value" : "suggestion1",
            "tc:description" : "A description of suggestion 1"
        },
        {
            "tc:value" : "suggestion2",
            "tc:description" : "A description of suggestion 2"
        }
    ]
}
```


# Building from Source Code

* Requirements: Git, Apache Maven 3, Java JDK 7

```bash
$ git clone https://github.com/BCDH/TEI-Completer.git
$ cd TEI-Completer.git
$ mvn package
```

The compiled uber jar file can then be found at `target/tei-completer-1.0-SNAPSHOT-shaded.jar`.


