package cz.inqool.arclib.dto;

import cz.inqool.arclib.dto.NodeStateDto;
import cz.inqool.arclib.storage.BasicStateInfo;
import cz.inqool.arclib.storage.StorageType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class StorageStateDto extends BasicStateInfo {

    private StorageType type;

    private List<NodeStateDto> nodes = new ArrayList<>();

    public void addNode(NodeStateDto node){
        this.nodes.add(node);
    }

    public StorageStateDto(long capacity, long free, boolean running, StorageType type){
        super(capacity,free,running);
        this.setType(type);
    }
}
