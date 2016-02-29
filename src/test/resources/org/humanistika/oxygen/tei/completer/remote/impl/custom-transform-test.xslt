<?xml version="1.0" encoding="UTF-8"?>
<!--
    Example of a JSON Response transformation written
    in XSLT
    
    The `content` param is a JSON object in the format
    described in: https://github.com/BCDH/TEI-Completer#server-messages
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:tc="http://humanistika.org/ns/tei-completer"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:output indent="yes" omit-xml-declaration="no" encoding="UTF-8"/>

    <xsl:template match="sgns">
        <tc:suggestions>
            <xsl:apply-templates/>
        </tc:suggestions>
    </xsl:template>
    
    <xsl:template match="sgn">
        <tc:suggestion>
            <xsl:apply-templates/>
        </tc:suggestion>
    </xsl:template>
    
    <xsl:template match="v">
        <tc:value><xsl:value-of select="."/></tc:value>
    </xsl:template>
    
    <xsl:template match="d">
        <tc:description><xsl:value-of select="."/></tc:description>
    </xsl:template>
    
</xsl:stylesheet>