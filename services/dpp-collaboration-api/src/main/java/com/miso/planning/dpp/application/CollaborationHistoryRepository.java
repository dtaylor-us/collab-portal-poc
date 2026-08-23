package com.miso.planning.dpp.application;
import com.miso.planning.dpp.domain.Correction;
import com.miso.planning.dpp.domain.MisoDisposition;
import java.util.List;
import java.util.UUID;
public interface CollaborationHistoryRepository {
  Correction addCorrection(UUID reviewId, String comment);
  List<Correction> corrections(UUID reviewId);
  MisoDisposition addDisposition(UUID reviewId, int correctionVersion, MisoDisposition.Decision decision, String comment);
  List<MisoDisposition> dispositions(UUID reviewId);
}
