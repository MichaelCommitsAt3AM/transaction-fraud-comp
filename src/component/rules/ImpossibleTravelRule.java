package component.rules;

import component.api.IFraudRule;
import component.core.Transaction;
import component.core.TransactionContext;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Flags transactions that require impossible travel.
 * If the speed between the last transaction and the current one
 * exceeds a configurable maximum, it flags fraud.
 */
public class ImpossibleTravelRule implements IFraudRule {

    private static final double MAX_ALLOWED_SPEED_KMH = 900.0; // Max commercial flight speed

    @Override
    public boolean isFraudulent(TransactionContext context) {
        List<Transaction> history = context.getHistory();
        if (history.isEmpty()) return false; // No prior transaction to compare

        Transaction lastTx = history.get(history.size() - 1);

        // Extract coordinates
        double prevLat = lastTx.getLatitude();
        double prevLon = lastTx.getLongitude();
        double currLat = context.getLatitude();
        double currLon = context.getLongitude();

        // If coordinates are missing or identical, skip check
        if ((prevLat == 0 && prevLon == 0) || (currLat == 0 && currLon == 0)) return false;
        if (prevLat == currLat && prevLon == currLon) return false;

        // Time difference in hours
        long secondsDiff = ChronoUnit.SECONDS.between(lastTx.getTimestamp(), LocalDateTime.now());
        if (secondsDiff <= 0) secondsDiff = 1; // avoid division by zero
        double hoursDiff = secondsDiff / 3600.0;

        // Distance in km using Haversine formula
        double distanceKm = haversineDistance(prevLat, prevLon, currLat, currLon);

        // Calculate travel speed
        double speedKmh = distanceKm / hoursDiff;

        // DEBUG: Print info
        System.out.println(String.format("   [DEBUG] Travel: %.6f,%.6f -> %.6f,%.6f | Dist: %.1f km | Time: %.2f hrs | Speed: %.1f km/h",
                prevLat, prevLon, currLat, currLon, distanceKm, hoursDiff, speedKmh));

        return speedKmh > MAX_ALLOWED_SPEED_KMH;
    }

    @Override
    public String getRuleName() {
        return "Impossible Travel Detected (Speed check)";
    }

    // Haversine formula
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
