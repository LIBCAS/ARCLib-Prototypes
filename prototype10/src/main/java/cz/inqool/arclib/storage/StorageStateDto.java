package cz.inqool.arclib.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class StorageStateDto extends BasicStateInfo{
    @Getter
    @Setter
    private StorageType type;
    private List<NodeStateDto> nodes = new ArrayList<>();

    public List<NodeStateDto> getNodes(){
        return Collections.unmodifiableList(this.nodes);
    }

    public void addNode(NodeStateDto node){
        this.nodes.add(node);
    }

    public StorageStateDto(long capacity, long free, boolean running, StorageType type){
        super(capacity,free,running);
        this.setType(type);
        this.setRunning(running);
    }
}
