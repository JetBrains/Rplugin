// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: classes.proto

package org.jetbrains.r.classes;

public interface S4ClassInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:classes.S4ClassInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string className = 1;</code>
   * @return The className.
   */
  java.lang.String getClassName();
  /**
   * <code>string className = 1;</code>
   * @return The bytes for className.
   */
  com.google.protobuf.ByteString
      getClassNameBytes();

  /**
   * <code>string packageName = 2;</code>
   * @return The packageName.
   */
  java.lang.String getPackageName();
  /**
   * <code>string packageName = 2;</code>
   * @return The bytes for packageName.
   */
  com.google.protobuf.ByteString
      getPackageNameBytes();

  /**
   * <code>repeated .classes.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  java.util.List<org.jetbrains.r.classes.S4ClassInfo.S4ClassSlot> 
      getSlotsList();
  /**
   * <code>repeated .classes.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  org.jetbrains.r.classes.S4ClassInfo.S4ClassSlot getSlots(int index);
  /**
   * <code>repeated .classes.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  int getSlotsCount();
  /**
   * <code>repeated .classes.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  java.util.List<? extends org.jetbrains.r.classes.S4ClassInfo.S4ClassSlotOrBuilder> 
      getSlotsOrBuilderList();
  /**
   * <code>repeated .classes.S4ClassInfo.S4ClassSlot slots = 3;</code>
   */
  org.jetbrains.r.classes.S4ClassInfo.S4ClassSlotOrBuilder getSlotsOrBuilder(
      int index);

  /**
   * <code>repeated .classes.S4ClassInfo.S4SuperClass superClasses = 4;</code>
   */
  java.util.List<org.jetbrains.r.classes.S4ClassInfo.S4SuperClass> 
      getSuperClassesList();
  /**
   * <code>repeated .classes.S4ClassInfo.S4SuperClass superClasses = 4;</code>
   */
  org.jetbrains.r.classes.S4ClassInfo.S4SuperClass getSuperClasses(int index);
  /**
   * <code>repeated .classes.S4ClassInfo.S4SuperClass superClasses = 4;</code>
   */
  int getSuperClassesCount();
  /**
   * <code>repeated .classes.S4ClassInfo.S4SuperClass superClasses = 4;</code>
   */
  java.util.List<? extends org.jetbrains.r.classes.S4ClassInfo.S4SuperClassOrBuilder> 
      getSuperClassesOrBuilderList();
  /**
   * <code>repeated .classes.S4ClassInfo.S4SuperClass superClasses = 4;</code>
   */
  org.jetbrains.r.classes.S4ClassInfo.S4SuperClassOrBuilder getSuperClassesOrBuilder(
      int index);

  /**
   * <code>bool isVirtual = 5;</code>
   * @return The isVirtual.
   */
  boolean getIsVirtual();
}
