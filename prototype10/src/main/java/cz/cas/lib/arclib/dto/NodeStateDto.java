package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.storage.BasicStateInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeStateDto extends BasicStateInfo {
    String id;
    String ip;

    public NodeStateDto(long capacity, long free, boolean running, String id, String ip) {
        super(capacity, free, running);
        this.setId(id);
        this.setIp(ip);
    }
}
