package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.BatchState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkerDto {
    private String batchId;

    private BatchState batchState;
}
