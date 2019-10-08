
package fi.soveltia.liferay.gsearch.geolocation.service;

/**
 * Geolocation service for resolving coordinates for an IP.
 * 
 * @author Petteri Karttunen
 */
public interface GeoLocationService {

	/**
	 * Get coordinates for and ip address. 
	 * 
	 * [0] = latitude [1] = longitude
	 * 
	 * @param ipAddress
	 * @return
	 */
	public Float[] getCoordinates(String ipAddress)
		throws Exception;

}
