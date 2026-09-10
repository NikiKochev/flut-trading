package com.niki.fluttrading.service;

import com.niki.fluttrading.contract.SchuurRequest;
import com.niki.fluttrading.service.impl.FlutInputFileParserImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlutInputFileParserImplTest {

    private final FlutInputFileParserImpl parser = new FlutInputFileParserImpl();

    @Test
    void parsesSingleTestCaseWithOneSchuur() {
        MockMultipartFile file = fileOf("""
                1
                6 12 3 10 7 16 5
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(1);
        SchuurRequest request = requests.getFirst();
        assertThat(request.isValid()).isTrue();
        assertThat(request.getFlutes()).hasSize(1);
        assertThat(request.getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);
    }

    @Test
    void parsesSingleTestCaseWithMultipleSchuurs() {
        MockMultipartFile file = fileOf("""
                2
                5 7 3 11 9 10
                9 1 2 3 4 10 16 10 4 16
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(1);
        SchuurRequest request = requests.getFirst();
        assertThat(request.isValid()).isTrue();
        assertThat(request.getFlutes()).hasSize(2);
        assertThat(request.getFlutes().getFirst().getPrices())
                .containsExactly(7, 3, 11, 9, 10);
        assertThat(request.getFlutes().get(1).getPrices())
                .containsExactly(1, 2, 3, 4, 10, 16, 10, 4, 16);
    }

    @Test
    void parsesMultipleTestCasesFromTheSameFile() {
        MockMultipartFile file = fileOf("""
                1
                6 12 3 10 7 16 5
                2
                5 7 3 11 9 10
                9 1 2 3 4 10 16 10 4 16
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(2);

        SchuurRequest first = requests.getFirst();
        assertThat(first.isValid()).isTrue();
        assertThat(first.getFlutes()).hasSize(1);
        assertThat(first.getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);

        SchuurRequest second = requests.get(1);
        assertThat(second.isValid()).isTrue();
        assertThat(second.getFlutes()).hasSize(2);
        assertThat(second.getFlutes().getFirst().getPrices())
                .containsExactly(7, 3, 11, 9, 10);
        assertThat(second.getFlutes().get(1).getPrices())
                .containsExactly(1, 2, 3, 4, 10, 16, 10, 4, 16);
    }

    @Test
    void ignoresBlankLinesBetweenTestCases() {
        MockMultipartFile file = fileOf("""
                
                1
                
                6 12 3 10 7 16 5
                
                0
                
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);
    }

    @Test
    void continuesProcessingFurtherTestCasesEvenAfterALineOfZero() {
        MockMultipartFile file = fileOf("""
                2
                5 7 3 11 9 10
                9 1 2 3 4 10 16 10 4 16
                0
                1
                6 12 3 10 7 16 5
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(2);

        SchuurRequest first = requests.getFirst();
        assertThat(first.isValid()).isTrue();
        assertThat(first.getFlutes()).hasSize(2);
        assertThat(first.getFlutes().getFirst().getPrices())
                .containsExactly(7, 3, 11, 9, 10);
        assertThat(first.getFlutes().get(1).getPrices())
                .containsExactly(1, 2, 3, 4, 10, 16, 10, 4, 16);

        SchuurRequest second = requests.get(1);
        assertThat(second.isValid()).isTrue();
        assertThat(second.getFlutes()).hasSize(1);
        assertThat(second.getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);
    }

    @Test
    void marksTestCaseInvalidWhenDeclaredBoxCountDoesNotMatchActualPrices() {
        MockMultipartFile file = fileOf("""
                1
                6 12 3 10 7 16
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(1);
        SchuurRequest request = requests.getFirst();
        assertThat(request.isValid()).isFalse();
        assertThat(request.getFailureMessage()).contains("Invalid line format");
        assertThat(request.getFlutes()).isEmpty();
    }

    @Test
    void marksTestCaseInvalidWhenPriceIsNegative() {
        MockMultipartFile file = fileOf("""
                1
                5 7 3 -11 9 10
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(1);
        SchuurRequest request = requests.getFirst();
        assertThat(request.isValid()).isFalse();
        assertThat(request.getFailureMessage()).contains("Invalid line format");
        assertThat(request.getFlutes()).isEmpty();
    }

    @Test
    void throwsExceptionForEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "input.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsExceptionForNullFile() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsExceptionForNonNumericTokenInPileLine() {
        MockMultipartFile file = fileOf("""
                1
                6 12 3 abc 7 16 5
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();
        assertThat(requests).hasSize(1);
        SchuurRequest request = requests.getFirst();
        assertThat(request.isValid()).isFalse();
        assertThat(request.getFailureMessage()).contains("Invalid line format");
        assertThat(request.getFlutes()).isEmpty();
    }

    @Test
    void parsesCombinedFileWithLeadingZeroTwoSchuursNegativePriceLettersAndValidCases() {
        MockMultipartFile file = fileOf("""
                0
                2
                9 1 2 3 4 10 16 10 4 16
                5 7 3 -11 9 10
                1
                6 12 3 10 7 16 5
                1
                6 12 3 abc 7 16 5
                1
                6 12 3 10 7 16 5
                0
                """);

        List<SchuurRequest> requests = parser.parse(file).getSchuurs();

        assertThat(requests).hasSize(4);

        SchuurRequest negativePriceCase = requests.getFirst();
        assertThat(negativePriceCase.isValid()).isFalse();
        assertThat(negativePriceCase.getFailureMessage()).contains("Invalid line format");
        assertThat(negativePriceCase.getFlutes()).hasSize(1);
        assertThat(negativePriceCase.getFlutes().getFirst().getPrices())
                .containsExactly(1, 2, 3, 4, 10, 16, 10, 4, 16);

        SchuurRequest firstOkCase = requests.get(1);
        assertThat(firstOkCase.isValid()).isTrue();
        assertThat(firstOkCase.getFlutes()).hasSize(1);
        assertThat(firstOkCase.getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);

        SchuurRequest lettersCase = requests.get(2);
        assertThat(lettersCase.isValid()).isFalse();
        assertThat(lettersCase.getFailureMessage()).contains("Invalid line format");
        assertThat(lettersCase.getFlutes()).isEmpty();

        SchuurRequest secondOkCase = requests.get(3);
        assertThat(secondOkCase.isValid()).isTrue();
        assertThat(secondOkCase.getFlutes()).hasSize(1);
        assertThat(secondOkCase.getFlutes().getFirst().getPrices())
                .containsExactly(12, 3, 10, 7, 16, 5);
    }

    private MockMultipartFile fileOf(String content) {
        return new MockMultipartFile(
                "file", "input.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

}
