package com.quip.backend.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        String[] array = parameter.toArray(new String[0]);
        ps.setArray(i, ps.getConnection().createArrayOf("text", array));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        if (array == null) {
            return null;
        }
        return new ArrayList<>(Arrays.asList((String[]) array.getArray()));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        if (array == null) {
            return null;
        }
        return new ArrayList<>(Arrays.asList((String[]) array.getArray()));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        if (array == null) {
            return null;
        }
        return new ArrayList<>(Arrays.asList((String[]) array.getArray()));
    }
}
