package org.gdjclouds.provider.gdnova.v100;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jclouds.openstack.nova.domain.ImageStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Image extends org.jclouds.openstack.nova.domain.Resource {
    private String id;
    private String name;
    private Integer progress;
    private String serverRef;
    private ImageStatus status;
    private Map<String, String> metadata = Maps.newHashMap();

    private Date created;
    private Date updated;

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }


    public Image() {
    }

    public Image(String id, String name) {
        this.id = id;
        this.name = name;
    }


    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setServerRef(String serverRef) {
        this.serverRef = serverRef;
    }

    public String getServerRef() {
        return serverRef;
    }

    public void setStatus(ImageStatus status) {
        this.status = status;
    }

    public ImageStatus getStatus() {
        return status;
    }


    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = Maps.newHashMap(metadata);
    }

    /**
     * note that this ignores some fields
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 :id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((serverRef == null) ? 0 : serverRef.hashCode());
        return result;
    }

    /**
     * note that this ignores some fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Image other = (Image) obj;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Image [created=" + getCreated() + ", id=" + id + ", name=" + name + ", serverRef="
                + serverRef + "]";
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
