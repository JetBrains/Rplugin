// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

public interface S4ClassInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rplugininterop.S4ClassInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string className = 1;</code>
   */
  java.lang.String getClassName();
  /**
   * <code>string className = 1;</code>
   */
  com.google.protobuf.ByteString
      getClassNameBytes();

  /**
   * <code>string packageName = 2;</code>
   */
  java.lang.String getPackageName();
  /**
   * <code>string packageName = 2;</code>
   */
  com.google.protobuf.ByteString
      getPackageNameBytes();

  /**
   * <code>repeated .rplugininterop.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  java.util.List<org.jetbrains.r.rinterop.S4ClassInfo.S4ClassSlot> 
      getSlotsList();
  /**
   * <code>repeated .rplugininterop.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  org.jetbrains.r.rinterop.S4ClassInfo.S4ClassSlot getSlots(int index);
  /**
   * <code>repeated .rplugininterop.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  int getSlotsCount();
  /**
   * <code>repeated .rplugininterop.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  java.util.List<? extends org.jetbrains.r.rinterop.S4ClassInfo.S4ClassSlotOrBuilder> 
      getSlotsOrBuilderList();
  /**
   * <code>repeated .rplugininterop.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  org.jetbrains.r.rinterop.S4ClassInfo.S4ClassSlotOrBuilder getSlotsOrBuilder(
      int index);

  /**
   * <code>repeated string superClasses = 4;</code>
   */
  java.util.List<java.lang.String>
      getSuperClassesList();
  /**
   * <code>repeated string superClasses = 4;</code>
   */
  int getSuperClassesCount();
  /**
   * <code>repeated string superClasses = 4;</code>
   */
  java.lang.String getSuperClasses(int index);
  /**
   * <code>repeated string superClasses = 4;</code>
   */
  com.google.protobuf.ByteString
      getSuperClassesBytes(int index);

  /**
   * <code>bool isVirtual = 5;</code>
   */
  boolean getIsVirtual();
}