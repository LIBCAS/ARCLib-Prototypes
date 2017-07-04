package cz.inqool.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RemoteFileIdDto {
    private String serverIp;
    private String filePath;
}
