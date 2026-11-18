package payroll.dao;

import payroll.PayrollResult;
import java.util.List;

public interface PayrollResultDAO {
    void saveBatch(String runId, List<PayrollResult> results) throws DataAccessException;
    List<PayrollResult> findByRunId(String runId) throws DataAccessException;
}
