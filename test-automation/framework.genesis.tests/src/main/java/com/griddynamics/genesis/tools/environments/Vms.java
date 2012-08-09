package com.griddynamics.genesis.tools.environments;


public class Vms {
    String envName;
    String roleName;
    String hostNumber;
    String instanceId;
    String hardwareId;
    String imageId;
    String publicIp;
    String privateIp;
    String status;

    public Vms() {
    }

    public Vms(String envName) {
        this.envName = envName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getHostNumber() {
        return hostNumber;
    }

    public void setHostNumber(String hostNumber) {
        this.hostNumber = hostNumber;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vms vms = (Vms) o;

        if (envName != null ? !envName.equals(vms.envName) : vms.envName != null) return false;
        if (hardwareId != null ? !hardwareId.equals(vms.hardwareId) : vms.hardwareId != null) return false;
        if (hostNumber != null ? !hostNumber.equals(vms.hostNumber) : vms.hostNumber != null) return false;
        if (imageId != null ? !imageId.equals(vms.imageId) : vms.imageId != null) return false;
        if (instanceId != null ? !instanceId.equals(vms.instanceId) : vms.instanceId != null) return false;
        if (privateIp != null ? !privateIp.equals(vms.privateIp) : vms.privateIp != null) return false;
        if (publicIp != null ? !publicIp.equals(vms.publicIp) : vms.publicIp != null) return false;
        if (roleName != null ? !roleName.equals(vms.roleName) : vms.roleName != null) return false;
        if (status != null ? !status.equals(vms.status) : vms.status != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = envName != null ? envName.hashCode() : 0;
        result = 31 * result + (roleName != null ? roleName.hashCode() : 0);
        result = 31 * result + (hostNumber != null ? hostNumber.hashCode() : 0);
        result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
        result = 31 * result + (hardwareId != null ? hardwareId.hashCode() : 0);
        result = 31 * result + (imageId != null ? imageId.hashCode() : 0);
        result = 31 * result + (publicIp != null ? publicIp.hashCode() : 0);
        result = 31 * result + (privateIp != null ? privateIp.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }
}
