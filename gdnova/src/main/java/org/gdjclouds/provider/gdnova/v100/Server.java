package org.gdjclouds.provider.gdnova.v100;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jclouds.openstack.nova.domain.Addresses;
import org.jclouds.openstack.nova.domain.ServerStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Server extends org.jclouds.openstack.nova.domain.Resource {

    private String id;
    private String name;

    private Map<String, String> metadata = Maps.newHashMap();

    private Addresses addresses;
    private String adminPass;
    private String flavorRef;
    private String hostId;
    private String imageRef;
    private String affinityId;

    private Date created;
    private Date updated;

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }


    private Integer progress;
    private ServerStatus status;

    public Server() {
    }

    public Server(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getAffinityId() {
        return affinityId;
    }

    public void setAffinityId(String affinityId) {
        this.affinityId = affinityId;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setAddresses(Addresses addresses) {
        this.addresses = addresses;
    }

    public Addresses getAddresses() {
        return addresses;
    }

    public void setAdminPass(String adminPass) {
        this.adminPass = adminPass;
    }

    public String getAdminPass() {
        return adminPass;
    }

    public void setFlavorRef(String flavorRef) {
        this.flavorRef = flavorRef;
    }

    public String getFlavorRef() {
        return flavorRef;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    /**
     * The OpenStack Nova provisioning algorithm has an anti-affinity property that attempts to spread
     * out customer VMs across hosts. Under certain situations, VMs from the same customer may be
     * placed on the same host. hostId represents the host your cloud server runs on and can be used
     * to determine this scenario if it's relevant to your application.
     * <p/>
     * Note: hostId is unique PER ACCOUNT and is not globally unique.
     */
    public String getHostId() {
        return hostId;
    }

    public String getId() {
        return id;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    public String getImageRef() {
        return imageRef;
    }

    public String getName() {
        return name;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    /**
     * Servers contain a status attribute that can be used as an indication of the current server
     * state. Servers with an ACTIVE status are available for use.
     */
    public ServerStatus getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
        result = prime * result + ((adminPass == null) ? 0 : adminPass.hashCode());
        result = prime * result + ((flavorRef == null) ? 0 : flavorRef.hashCode());
        result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
        result = prime * result + ((id == null)? 0 : id.hashCode());
        result = prime * result + ((imageRef == null) ? 0 : imageRef.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Server other = (Server) obj;
        if (addresses == null) {
            if (other.addresses != null)
                return false;
        } else if (!addresses.equals(other.addresses))
            return false;
        if (adminPass == null) {
            if (other.adminPass != null)
                return false;
        } else if (!adminPass.equals(other.adminPass))
            return false;
        if (flavorRef == null) {
            if (other.flavorRef != null)
                return false;
        } else if (!flavorRef.equals(other.flavorRef))
            return false;
        if (hostId == null) {
            if (other.hostId != null)
                return false;
        } else if (!hostId.equals(other.hostId))
            return false;
        if (id != other.id)
            return false;
        if (imageRef == null) {
            if (other.imageRef != null)
                return false;
        } else if (!imageRef.equals(other.imageRef))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Server [addresses=" + addresses + ", adminPass=" + adminPass + ", flavorRef="
                + flavorRef + ", hostId=" + hostId + ", id=" + id + ", imageRef=" + imageRef
                + ", metadata=" + metadata + ", name=" + name + "]";
    }

    private List<Map<String, String>> links = Lists.newArrayList();

    @Override
    public URI getURI() {
        for (Map<String, String> linkProperties : links) {
            try {
                if (!Functions.forMap(linkProperties, "").apply("rel").equals("bookmark"))
                    continue;
                return new URI(linkProperties.get("href"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("URI is not available");

    }
}
