/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.jobs.v2.JobService;
import com.google.api.services.jobs.v2.model.Company;
import com.google.api.services.jobs.v2.model.CreateJobRequest;
import com.google.api.services.jobs.v2.model.Empty;
import com.google.api.services.jobs.v2.model.Job;
import com.google.api.services.jobs.v2.model.UpdateJobRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The samples in this file introduce how to do batch operation in CJD. Including:
 *
 * - Create job within batch
 *
 * - Update job within batch
 *
 * - Delete job within batch.
 *
 * For simplicity, the samples always use the same kind of requests in each batch. In a real case ,
 * you might put different kinds of request in one batch.
 */
public final class BatchOperationSample {

  private static JobService jobService = JobServiceUtils.getJobService();

  // [START batch_job_create]
  public static List<Job> batchCreateJobs(String companyName) throws IOException {
    List<Job> createdJobs = new ArrayList<>();

    // Callback for batch create
    JsonBatchCallback<Job> createCallback =
        new JsonBatchCallback<Job>() {
          @Override
          public void onFailure(GoogleJsonError e,
              HttpHeaders responseHeaders) {
            System.out.println("Create Error Message: " + e.getMessage());
          }

          @Override
          public void onSuccess(Job response, HttpHeaders responseHeaders) {
            System.out.println("Create Job: " + response);
            createdJobs.add(response);
          }
        };

    Job softwareEngineerJob =
        new Job()
            .setCompanyName(companyName)
            .setRequisitionId("123456")
            .setJobTitle("Software Engineer")
            .setApplicationUrls(Arrays.asList("http://careers.google.com"))
            .setDescription(
                "Design, develop, test, deploy, maintain and improve software.");
    Job hardwareEngineerJob =
        new Job()
            .setCompanyName(companyName)
            .setRequisitionId("1234567")
            .setJobTitle("Hardware Engineer")
            .setApplicationUrls(Arrays.asList("http://careers.google.com"))
            .setDescription(
                "Design prototype PCBs or modify existing board designs "
                    + "to prototype new features or functions.");

    // Creates batch request
    BatchRequest batchCreate = jobService.batch();

    // Queues create job request
    jobService
        .jobs()
        .create(new CreateJobRequest().setJob(softwareEngineerJob))
        .queue(batchCreate, createCallback);
    jobService
        .jobs()
        .create(new CreateJobRequest().setJob(hardwareEngineerJob))
        .queue(batchCreate, createCallback);

    // Executes batch request
    batchCreate.execute();
    return createdJobs;
  }
  // [END batch_job_create]

  // [START batch_job_update]
  public static List<Job> batchJobUpdate(List<Job> jobsToBeUpdate) throws IOException {
    List<Job> updatedJobs = new ArrayList<>();

    JsonBatchCallback<Job> updateCallback =
        new JsonBatchCallback<Job>() {
          @Override
          public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
            System.out.println("Update Error Message: " + e.getMessage());
          }

          @Override
          public void onSuccess(Job job, HttpHeaders responseHeaders) {
            System.out.println("Update Job: " + job);
            updatedJobs.add(job);
          }
        };

    BatchRequest batchUpdate = jobService.batch();
    // You might use Job entity with all fields filled in to do the update
    for (int i = 0; i < jobsToBeUpdate.size(); i += 2) {
      Job toBeUpdated = jobsToBeUpdate.get(i);
      toBeUpdated.setJobTitle("Engineer in Mountain View");
      jobService
          .jobs()
          .patch(toBeUpdated.getName(), new UpdateJobRequest().setJob(toBeUpdated))
          .queue(batchUpdate, updateCallback);
    }
    // Or just fill in part of field in Job entity and set the updateJobFields
    for (int i = 1; i < jobsToBeUpdate.size(); i += 2) {
      Job toBeUpdated = new Job().setJobTitle("Engineer in Mountain View")
          .setName(jobsToBeUpdate.get(i).getName());
      jobService
          .jobs()
          .patch(toBeUpdated.getName(),
              new UpdateJobRequest().setJob(toBeUpdated).setUpdateJobFields("jobTitle"))
          .queue(batchUpdate, updateCallback);
    }
    batchUpdate.execute();

    return updatedJobs;
  }

  // [END batch_job_update]

  // [START batch_job_delete]
  public static void batchDeleteJobs(List<Job> jobsToBeDeleted) throws IOException {
    BatchRequest batchDelete = jobService.batch();
    for (Job job : jobsToBeDeleted) {
      jobService
          .jobs()
          .delete(job.getName())
          .queue(
              batchDelete,
              new JsonBatchCallback<Empty>() {
                @Override
                public void onFailure(GoogleJsonError e,
                    HttpHeaders responseHeaders) {
                  System.out.println("Delete Error Message: " + e.getMessage());
                }

                @Override
                public void onSuccess(Empty empty, HttpHeaders responseHeaders) {
                  System.out.println("Job deleted");
                }
              });
      batchDelete.execute();
    }
  }
  // [END batch_job_delete]

  public static void main(String... args) throws Exception {
    Company company = BasicCompanySample
        .createCompany(BasicCompanySample.generateCompany());

    // Batch create jobs
    List<Job> createdJobs = batchCreateJobs(company.getName());

    // Batch update jobs
    List<Job> updatedJobs = batchJobUpdate(createdJobs);

    // Batch delete jobs
    batchDeleteJobs(updatedJobs);

    BasicCompanySample.deleteCompany(company.getName());
  }
}
