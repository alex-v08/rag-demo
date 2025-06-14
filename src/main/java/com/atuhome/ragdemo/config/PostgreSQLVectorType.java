package com.atuhome.ragdemo.config;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PostgreSQLVectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        if (x == y) return true;
        if (x == null || y == null) return false;
        if (x.length != y.length) return false;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != y[i]) return false;
        }
        return true;
    }

    @Override
    public int hashCode(float[] x) {
        if (x == null) return 0;
        int result = 1;
        for (float element : x) {
            result = 31 * result + Float.hashCode(element);
        }
        return result;
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(position);
        if (value == null || rs.wasNull()) {
            return null;
        }
        
        // Parse PostgreSQL vector format: [1.0,2.0,3.0]
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        
        if (value.isEmpty()) {
            return new float[0];
        }
        
        String[] parts = value.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            // Convert to PostgreSQL vector format
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < value.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(value[i]);
            }
            sb.append("]");
            st.setObject(index, sb.toString(), Types.OTHER);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        if (value == null) return null;
        return value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached);
    }

    @Override
    public float[] replace(float[] detached, float[] managed, Object owner) {
        return deepCopy(detached);
    }
}