// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

public interface DataFrameGetDataRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rplugininterop.DataFrameGetDataRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.rplugininterop.RRef ref = 1;</code>
   * @return Whether the ref field is set.
   */
  boolean hasRef();
  /**
   * <code>.rplugininterop.RRef ref = 1;</code>
   * @return The ref.
   */
  org.jetbrains.r.rinterop.RRef getRef();
  /**
   * <code>.rplugininterop.RRef ref = 1;</code>
   */
  org.jetbrains.r.rinterop.RRefOrBuilder getRefOrBuilder();

  /**
   * <code>int32 start = 2;</code>
   * @return The start.
   */
  int getStart();

  /**
   * <code>int32 end = 3;</code>
   * @return The end.
   */
  int getEnd();
}
