package org.camunda.optimize.service.importing;

import org.camunda.optimize.service.exceptions.OptimizeException;
import org.camunda.optimize.service.importing.impl.PaginatedImportService;
import org.camunda.optimize.service.importing.index.DefinitionBasedImportIndexHandler;
import org.camunda.optimize.service.importing.index.ImportIndexHandler;
import org.camunda.optimize.service.importing.job.schedule.IdBasedImportScheduleJob;
import org.camunda.optimize.service.importing.job.schedule.ImportScheduleJob;
import org.camunda.optimize.service.importing.provider.ImportServiceProvider;
import org.camunda.optimize.service.importing.provider.IndexHandlerProvider;
import org.camunda.optimize.service.status.ImportProgressReporter;
import org.camunda.optimize.service.util.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/unit/applicationContext.xml"})
public class ImportSchedulerTest extends AbstractSchedulerTest {

  public static final String TEST_ID = "testId";
  @Autowired
  private ImportServiceProvider importServiceProvider;

  @Autowired
  private IndexHandlerProvider indexHandlerProvider;

  @Autowired
  private ImportScheduler importScheduler;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private ImportProgressReporter importProgressReporter;

  private List<PaginatedImportService> services;

  @Before
  public void setUp() throws OptimizeException {
    importScheduler.importScheduleJobs.clear();

    services = mockImportServices();

    mockIndexHandlers(services, indexHandlerProvider);

    when(importServiceProvider.getPagedServices()).thenReturn(services);
    when(importServiceProvider.getImportService(Mockito.any())).thenReturn(services.get(0));
  }



  @After
  public void tearDown() {
    Mockito.reset(importServiceProvider);
    Mockito.reset(indexHandlerProvider);
  }

  @Test
  public void testJobsScheduled () {
    importScheduler.scheduleNewImportRound();
    assertThat(importScheduler.importScheduleJobs.size(),is(importServiceProvider.getPagedServices().size()));
  }

  @Test
  public void allImportsAreTriggered() throws InterruptedException, OptimizeException {

    // given
    List<PaginatedImportService> services = mockImportServices();
    when(importServiceProvider.getPagedServices()).thenReturn(services);
    importScheduler.scheduleNewImportRound();

    // when
    importScheduler.executeNextJob();
    importScheduler.executeNextJob();
    importScheduler.executeNextJob();

    // then
    for (ImportService service : services) {
      verify(service, times(1)).executeImport(Mockito.any());
    }
  }

  @Test
  public void testProcessInstanceImportScheduledBasedOnActivities () throws Exception {
    PaginatedImportService paginatedImportService = services.get(0);

    ImportResult result = getConstructResult(paginatedImportService);

    when(paginatedImportService.executeImport(Mockito.any())).thenReturn(result);
    importScheduler.scheduleNewImportRound();

    //when
    importScheduler.executeNextJob();
    importScheduler.executeNextJob();
    importScheduler.executeNextJob();

    assertThat(importScheduler.importScheduleJobs.size(), is(5));
    ImportScheduleJob piJob = importScheduler.importScheduleJobs.poll();
    assertThat(piJob, is(instanceOf(IdBasedImportScheduleJob.class)));
    assertThat(((IdBasedImportScheduleJob)piJob).getIdsToFetch(), is(notNullValue()));
    assertThat(((IdBasedImportScheduleJob)piJob).getIdsToFetch().toArray()[0], is(TEST_ID));
  }

  private ImportResult getConstructResult(PaginatedImportService paginatedImportService) {
    ImportResult result = new ImportResult();
    result.setEngineHasStillNewData(true);
    result.setSearchedSize(1);
    Set<String> piIds = new HashSet<>();
    piIds.add(TEST_ID);
    result.setIdsToFetch(piIds);
    result.setElasticSearchType(paginatedImportService.getElasticsearchType());
    result.setIndexHandlerType(paginatedImportService.getIndexHandlerType());
    return result;
  }

  @Test
  public void testResetAfterPeriod () throws Exception {
    // given
    List<PaginatedImportService> services = mockImportServices();
    when(importServiceProvider.getPagedServices()).thenReturn(services);

    LocalDateTime expectedReset = LocalDateTime.now().plus(Double.valueOf(configurationService.getImportResetInterval()).longValue(), ChronoUnit.HOURS);
    long toSleep = LocalDateTime.now().until(expectedReset, ChronoUnit.MILLIS);
    
    //when
    Thread.currentThread().sleep(Math.max(toSleep,1000L));
    importScheduler.checkAndResetImportIndexing();

    // then
    Mockito.verify(
        indexHandlerProvider.getIndexHandler(
            services.get(0).getElasticsearchType(),
            services.get(0).getIndexHandlerType()
        ),
        times(1)
    ).resetImportIndex();
    assertThat(importScheduler.getLastReset().isAfter(LocalDateTime.now().minusSeconds(2)), is(true));

    //clean up mocks
    Mockito.reset(services.toArray());
  }

  @Test
  public void testResetIfEntitiesWereMissedDuringImport () throws Exception {
    // given
    List<PaginatedImportService> services = mockImportServices();
    when(importServiceProvider.getPagedServices()).thenReturn(services);
    when(importProgressReporter.allEntitiesAreImported()).thenReturn(true);

    //when
    importScheduler.checkAndResetImportIndexing();

    // then
    Mockito.verify(
        indexHandlerProvider.getIndexHandler(
            services.get(0).getElasticsearchType(),
            services.get(0).getIndexHandlerType()
        ),
        times(1)
    ).resetImportIndex();

    assertThat(importScheduler.getLastReset().isAfter(LocalDateTime.now().minusSeconds(2)), is(true));

    //clean up mocks
    Mockito.reset(services.toArray());
  }


}
