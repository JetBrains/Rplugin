// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

public interface VariablesResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rplugininterop.VariablesResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>bool isEnv = 1;</code>
   * @return The isEnv.
   */
  boolean getIsEnv();

  /**
   * <code>int64 totalCount = 2;</code>
   * @return The totalCount.
   */
  long getTotalCount();

  /**
   * <code>repeated .rplugininterop.VariablesResponse.Variable vars = 3;</code>
   */
  java.util.List<org.jetbrains.r.rinterop.VariablesResponse.Variable> 
      getVarsList();
  /**
   * <code>repeated .rplugininterop.VariablesResponse.Variable vars = 3;</code>
   */
  org.jetbrains.r.rinterop.VariablesResponse.Variable getVars(int index);
  /**
   * <code>repeated .rplugininterop.VariablesResponse.Variable vars = 3;</code>
   */
  int getVarsCount();
  /**
   * <code>repeated .rplugininterop.VariablesResponse.Variable vars = 3;</code>
   */
  java.util.List<? extends org.jetbrains.r.rinterop.VariablesResponse.VariableOrBuilder> 
      getVarsOrBuilderList();
  /**
   * <code>repeated .rplugininterop.VariablesResponse.Variable vars = 3;</code>
   */
  org.jetbrains.r.rinterop.VariablesResponse.VariableOrBuilder getVarsOrBuilder(
      int index);
}
