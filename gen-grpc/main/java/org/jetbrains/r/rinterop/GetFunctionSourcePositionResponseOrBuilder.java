// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

public interface GetFunctionSourcePositionResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rplugininterop.GetFunctionSourcePositionResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   * @return Whether the position field is set.
   */
  boolean hasPosition();
  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   * @return The position.
   */
  org.jetbrains.r.rinterop.SourcePosition getPosition();
  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   */
  org.jetbrains.r.rinterop.SourcePositionOrBuilder getPositionOrBuilder();

  /**
   * <code>string sourcePositionText = 2;</code>
   * @return The sourcePositionText.
   */
  java.lang.String getSourcePositionText();
  /**
   * <code>string sourcePositionText = 2;</code>
   * @return The bytes for sourcePositionText.
   */
  com.google.protobuf.ByteString
      getSourcePositionTextBytes();
}