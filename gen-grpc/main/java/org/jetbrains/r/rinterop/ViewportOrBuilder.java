// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

public interface ViewportOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rplugininterop.Viewport)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.rplugininterop.AffinePoint from = 1;</code>
   */
  boolean hasFrom();
  /**
   * <code>.rplugininterop.AffinePoint from = 1;</code>
   */
  org.jetbrains.r.rinterop.AffinePoint getFrom();
  /**
   * <code>.rplugininterop.AffinePoint from = 1;</code>
   */
  org.jetbrains.r.rinterop.AffinePointOrBuilder getFromOrBuilder();

  /**
   * <code>.rplugininterop.AffinePoint to = 2;</code>
   */
  boolean hasTo();
  /**
   * <code>.rplugininterop.AffinePoint to = 2;</code>
   */
  org.jetbrains.r.rinterop.AffinePoint getTo();
  /**
   * <code>.rplugininterop.AffinePoint to = 2;</code>
   */
  org.jetbrains.r.rinterop.AffinePointOrBuilder getToOrBuilder();
}