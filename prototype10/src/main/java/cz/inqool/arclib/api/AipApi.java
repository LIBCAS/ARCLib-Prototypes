package cz.inqool.arclib.api;

import cz.inqool.arclib.service.AipStateDto;
import cz.inqool.arclib.service.RemoteFileIdDto;
import cz.inqool.arclib.service.StorageStateDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storage/aip")
public class AipApi {


    /**
     * Retrieves specified AIP
     *
     * @param uuid unique AIP identifier
     */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public void get(@PathVariable("uuid") String uuid) {

    }

    /**
     * Copies packaged AIP from specified location and stores it into Archival Storage.
     * <p>
     * This endpoint also handles AIP versioning when whole AIP is versioned.
     *
     * @param remoteFileId contains IP of remote server and file path
     * @return
     */
    @RequestMapping(value = "/store", method = RequestMethod.POST)
    public void save(@RequestBody RemoteFileIdDto remoteFileId) {
    }

    /**
     * Copies ARCLib AIP XML from specified location and stores it into Archival Storage.
     *
     * @param remoteFileId contains IP of remote server and file path
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void updateXml(@RequestBody RemoteFileIdDto remoteFileId) {
    }


    /**
     * Logically removes AIP package by setting its state to <i><removed/i>.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public void remove(@PathVariable("uuid") String uuid) {

    }

    /**
     * Physically removes SIP part of AIP package and setts its state to <i><deleted/i>.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}/hard", method = RequestMethod.DELETE)
    public void delete(@PathVariable("uuid") String uuid) {

    }

    /**
     * Retrieves state of AIP.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}/state", method = RequestMethod.GET)
    public AipStateDto getAipState(@PathVariable("uuid") String uuid) {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves state of Archival Storage: its nodes, their states etc.
     *
     * @return
     */
    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public StorageStateDto getStorageState(@PathVariable("uuid") String uuid) {
        throw new UnsupportedOperationException();
    }

}
