package com.quip.backend.config.handler;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StringArrayTypeHandler}.
 * <p>
 * This test class validates the MyBatis type handler functionality for
 * converting between List<String> and SQL arrays.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StringArrayTypeHandler Tests")
class StringArrayTypeHandlerTest extends BaseTest {

    private StringArrayTypeHandler handler;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private Connection connection;

    @Mock
    private Array sqlArray;

    @BeforeEach
    void setUp() {
        handler = new StringArrayTypeHandler();
    }

    @Test
    @DisplayName("Should instantiate handler successfully")
    void shouldInstantiateHandler_Successfully() {
        // When & Then
        assertThat(handler).isNotNull();
    }

    @Nested
    @DisplayName("setNonNullParameter() Tests")
    class SetNonNullParameterTests {

        @Test
        @DisplayName("Should set parameter with string list successfully")
        void shouldSetParameter_WithStringList_Successfully() throws SQLException {
            // Given
            List<String> parameter = Arrays.asList("value1", "value2", "value3");
            when(preparedStatement.getConnection()).thenReturn(connection);
            when(connection.createArrayOf(eq("text"), any(String[].class))).thenReturn(sqlArray);

            // When
            handler.setNonNullParameter(preparedStatement, 1, parameter, null);

            // Then
            verify(preparedStatement).getConnection();
            verify(connection).createArrayOf(eq("text"), any(String[].class));
            verify(preparedStatement).setArray(1, sqlArray);
        }

        @Test
        @DisplayName("Should set parameter with empty list successfully")
        void shouldSetParameter_WithEmptyList_Successfully() throws SQLException {
            // Given
            List<String> parameter = Arrays.asList();
            when(preparedStatement.getConnection()).thenReturn(connection);
            when(connection.createArrayOf(eq("text"), any(String[].class))).thenReturn(sqlArray);

            // When
            handler.setNonNullParameter(preparedStatement, 1, parameter, null);

            // Then
            verify(preparedStatement).getConnection();
            verify(connection).createArrayOf(eq("text"), any(String[].class));
            verify(preparedStatement).setArray(1, sqlArray);
        }
    }

    @Nested
    @DisplayName("getNullableResult() Tests")
    class GetNullableResultTests {

        @Test
        @DisplayName("Should get result by column name successfully")
        void shouldGetResult_ByColumnName_Successfully() throws SQLException {
            // Given
            String columnName = "test_column";
            String[] arrayData = {"value1", "value2", "value3"};
            when(resultSet.getArray(columnName)).thenReturn(sqlArray);
            when(sqlArray.getArray()).thenReturn(arrayData);

            // When
            List<String> result = handler.getNullableResult(resultSet, columnName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsExactly("value1", "value2", "value3");
            verify(resultSet).getArray(columnName);
            verify(sqlArray).getArray();
        }

        @Test
        @DisplayName("Should return null when array is null by column name")
        void shouldReturnNull_WhenArrayIsNull_ByColumnName() throws SQLException {
            // Given
            String columnName = "test_column";
            when(resultSet.getArray(columnName)).thenReturn(null);

            // When
            List<String> result = handler.getNullableResult(resultSet, columnName);

            // Then
            assertThat(result).isNull();
            verify(resultSet).getArray(columnName);
            verify(sqlArray, never()).getArray();
        }

        @Test
        @DisplayName("Should get result by column index successfully")
        void shouldGetResult_ByColumnIndex_Successfully() throws SQLException {
            // Given
            int columnIndex = 1;
            String[] arrayData = {"value1", "value2"};
            when(resultSet.getArray(columnIndex)).thenReturn(sqlArray);
            when(sqlArray.getArray()).thenReturn(arrayData);

            // When
            List<String> result = handler.getNullableResult(resultSet, columnIndex);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsExactly("value1", "value2");
            verify(resultSet).getArray(columnIndex);
            verify(sqlArray).getArray();
        }

        @Test
        @DisplayName("Should return null when array is null by column index")
        void shouldReturnNull_WhenArrayIsNull_ByColumnIndex() throws SQLException {
            // Given
            int columnIndex = 1;
            when(resultSet.getArray(columnIndex)).thenReturn(null);

            // When
            List<String> result = handler.getNullableResult(resultSet, columnIndex);

            // Then
            assertThat(result).isNull();
            verify(resultSet).getArray(columnIndex);
            verify(sqlArray, never()).getArray();
        }

        @Test
        @DisplayName("Should get result from callable statement successfully")
        void shouldGetResult_FromCallableStatement_Successfully() throws SQLException {
            // Given
            int columnIndex = 1;
            String[] arrayData = {"value1"};
            when(callableStatement.getArray(columnIndex)).thenReturn(sqlArray);
            when(sqlArray.getArray()).thenReturn(arrayData);

            // When
            List<String> result = handler.getNullableResult(callableStatement, columnIndex);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsExactly("value1");
            verify(callableStatement).getArray(columnIndex);
            verify(sqlArray).getArray();
        }

        @Test
        @DisplayName("Should return null when array is null from callable statement")
        void shouldReturnNull_WhenArrayIsNull_FromCallableStatement() throws SQLException {
            // Given
            int columnIndex = 1;
            when(callableStatement.getArray(columnIndex)).thenReturn(null);

            // When
            List<String> result = handler.getNullableResult(callableStatement, columnIndex);

            // Then
            assertThat(result).isNull();
            verify(callableStatement).getArray(columnIndex);
            verify(sqlArray, never()).getArray();
        }

        @Test
        @DisplayName("Should handle empty array successfully")
        void shouldHandleEmptyArray_Successfully() throws SQLException {
            // Given
            String columnName = "test_column";
            String[] arrayData = {};
            when(resultSet.getArray(columnName)).thenReturn(sqlArray);
            when(sqlArray.getArray()).thenReturn(arrayData);

            // When
            List<String> result = handler.getNullableResult(resultSet, columnName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
            verify(resultSet).getArray(columnName);
            verify(sqlArray).getArray();
        }
    }
}
