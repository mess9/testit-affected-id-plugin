/*
 * API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v2.0
 *
 *
 * NOTE: This class is auto-generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.github.mess9.testitaffectedidplugin.testit.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;


@JsonAdapter(LinkType.Adapter.class)
public enum LinkType {

	RELATED("Related"),

	BLOCKEDBY("BlockedBy"),

	DEFECT("Defect"),

	ISSUE("Issue"),

	REQUIREMENT("Requirement"),

	REPOSITORY("Repository");

	private final String value;

	LinkType(String value) {
		this.value = value;
	}

	public static LinkType fromValue(String value) {
		for (LinkType b : LinkType.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static class Adapter extends TypeAdapter<LinkType> {

		@Override
		public void write(final JsonWriter jsonWriter, final LinkType enumeration) throws IOException {
			jsonWriter.value(enumeration.getValue());
		}

		@Override
		public LinkType read(final JsonReader jsonReader) throws IOException {
			String value = jsonReader.nextString();
			return LinkType.fromValue(value);
		}
	}

	public String getValue() {
		return value;
	}
}

