package Functions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReaderTest {

    @Test
    void formatElapsedTime_withValidValues_returnsFormattedString() {
        // Arrange
        long seconds = 3665; // 1 hour, 1 minute, and 5 seconds
        // Act
        String result = Reader.formatElapsedTime(seconds);
        // Assert
        assertEquals("1hr:1min:5sec", result);
    }

    @Test
    void formatElapsedTime_withZeroSeconds_returnsFormattedString() {
        // Arrange
        long seconds = 0;
        // Act
        String result = Reader.formatElapsedTime(seconds);
        // Assert
        assertEquals("0hr:0min:0sec", result);
    }

    @Test
    void formatElapsedTime_withNegativeSeconds_returnsFormattedString() {
        // Arrange
        long seconds = -500;
        // Act
        String result = Reader.formatElapsedTime(seconds);
        // Assert
        assertEquals("-1hr:0min:20sec", result);
    }
}

