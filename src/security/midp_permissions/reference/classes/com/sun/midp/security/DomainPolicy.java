/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.midp.security;


public class DomainPolicy {
    private String name;
    private boolean isTrusted;
    private byte[] defValues;
    private byte[] maxValues;
    
    public DomainPolicy(String name,  boolean isTrusted) {
        this.name = name;
        this.isTrusted = isTrusted;
        loadValues();
    }  
    
    public String getName() {
        return name;
    }
    
    public boolean isTrusted() {
        return isTrusted;
    }
    
    public void getPermissionlevels(byte [] list, int level){
        if (level == Permissions.CUR_LEVELS)
            System.arraycopy(defValues, 0, list, 0, defValues.length);
        else
            System.arraycopy(maxValues, 0, list, 0, maxValues.length);
    }
    
    public String toString() {
        return name + ((isTrusted)? ",trusted" : ",untrusted");
    }
    
    private void loadValues() {
        defValues = new byte[Permissions.NUMBER_OF_PERMISSIONS];
        maxValues = new byte[Permissions.NUMBER_OF_PERMISSIONS];
        defValues[0] = Permissions.NEVER;
        defValues[1] = Permissions.NEVER;
        maxValues[0] = Permissions.NEVER;
        maxValues[1] = Permissions.NEVER;
        for (int i1 = 2; i1 < defValues.length; i1++) {
            String group = Permissions.permissionSpecs[i1].group.getNativeName();
            defValues[i1] = Permissions.getDefaultValue(name, group);
            maxValues[i1] = Permissions.getMaxValue(name, group);
        }
    }
}
